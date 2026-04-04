package org.example.examsystem.service.Impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.examsystem.dto.AnswerDTO;
import org.example.examsystem.dto.ExamSubmitDTO;
import org.example.examsystem.dto.ExamSubmitMessage;
import org.example.examsystem.dto.QuestionAnswerDTO;
import org.example.examsystem.dto.StudentReviewCount;
import org.example.examsystem.entity.AnswerRecord;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.ExamQuestion;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.mapper.AnswerRecordMapper;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.ExamQuestionMapper;
import org.example.examsystem.mapper.QuestionMapper;
import org.example.examsystem.mapper.QuestionAnswerMapper;
import org.example.examsystem.mapper.QuestionMapper;
import org.example.examsystem.mapper.TesterExamMapper;
import org.example.examsystem.service.IService.IExamPaperService;
import org.example.examsystem.vo.ExamPaperDetailVO;
import org.example.examsystem.vo.PaperQuestionDetailVO;
import org.example.examsystem.vo.ProgressVO;
import org.example.examsystem.vo.QuestionDetailVO;
import org.example.examsystem.vo.QuestionSimpleInfoVO;
import org.example.examsystem.vo.TeacherReviewQuestionDetailVO;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl extends ServiceImpl<AnswerRecordMapper, AnswerRecord> implements IExamPaperService {

    private final AnswerRecordMapper answerRecordMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final TesterExamMapper  testerExamMapper;
    private final QuestionMapper questionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ExamMapper examMapper;
    private final QuestionAnswerMapper questionAnswerMapper;

    /**
     * 考生查看作答详细
     * @param examId 本场考试Id
     * @param userId 考生Id
     * @return 考试信息链表
     */
    @Override
    public List<QuestionDetailVO> getQuestionDetail(Long examId, Long userId) {
        return answerRecordMapper.getQuestionDetails(examId,userId);
    }

    /**
     * 获取本次考试题目Id --- 主观题
     * @param examId 考试Id
     * @return 题目链表
     */
    @Override
    public List<Long> getQuestionIdByExamId(Long examId) {
        return new LambdaQueryChainWrapper<>(examQuestionMapper)
                .eq(ExamQuestion::getExamId, examId)
                .eq(ExamQuestion::getIsDeleted, 0)
                .orderByAsc(ExamQuestion::getSort)
                .list()
                .stream()
                .map(ExamQuestion::getQuestionId)
                .toList();
    }

    /**
     * 获取本次考试需要人工批阅的题目Id（主观题）
     * @param examId 考试ID
     * @return 题目列表
     */
    @Override
    public List<Long> getSubjectiveQuestionIds(Long examId) {
        return examQuestionMapper.getSubjectiveQuestionIds(examId);
    }

    /**
     *  批阅时查询主观题详细
     * @param examId 考试ID
     * @param questionId 题目ID
     * @param studentId 考生ID
     * @return VO
     */
    @Override
    public TeacherReviewQuestionDetailVO getReviewQuestionDetail(Long examId, Long questionId, Long studentId) {
        return answerRecordMapper.getTeacherReviewDetail(examId,questionId,studentId);
    }

    /**
     * @param examId 考试ID
     * @param questionId 问题ID
     * @param studentId 考生ID
     * @param score 分数
     * @return success
     */
    @Override
    public Boolean judge(Long examId, Long questionId, Long studentId,Integer score) {
        return null;
    }

    /**
     * 获取评卷进度
     * @param examId 考试Id
     * @return ProgressVO
     */
    @Override
    public ProgressVO getReviewProgress(Long examId) {
        // 1. 总学生数
        long totalStudents = testerExamMapper.selectCount(
                Wrappers.<TesterExam>lambdaQuery()
                        .eq(TesterExam::getExamId, examId)
                        .eq(TesterExam::getIsDeleted, 0)
        );
        // 说明本次考试都是客观题
        if (totalStudents == 0) {
            return new ProgressVO(0, 0);
        }

        // 2. 考试中主观题数量（question_type = 5）
        Integer subjectiveCount = answerRecordMapper.countSubjectiveQuestions(examId);
        if (subjectiveCount == null || subjectiveCount == 0) {
            return new ProgressVO(totalStudents, totalStudents);
        }

        // 3. 每个学生已批阅的主观题数
        List<StudentReviewCount> reviewList = answerRecordMapper.countReviewedPerStudent(examId);

        // 4. 已批阅学生：已批主观题数 == 总主观题数
        long reviewedStudents = reviewList.stream()
                .filter(rc -> rc.getReviewedCount() != null && rc.getReviewedCount() == subjectiveCount)
                .count();

        return new ProgressVO(reviewedStudents, totalStudents);
    }


    /**
     * 答案保存进redis接口---暂存或最终提交
     */
    private void saveAnswers(ExamSubmitDTO dto,
                             TesterExam testerExam,
                             boolean finalSubmit) {

        ExamSubmitMessage msg = new ExamSubmitMessage();
        msg.setUserId(testerExam.getStudentId());
        msg.setExamId(dto.getExamId());
        msg.setSubmitTime(dto.getSubmitTime());
        msg.setAnswers(dto.getAnswers());
        msg.setAttemptId(testerExam.getId());
        msg.setFinalSubmit(finalSubmit);

        if (finalSubmit) {
            // 最终提交：存入队列
            redisTemplate.opsForList()
                    .leftPush("exam:submit:queue", JSONUtil.toJsonStr(msg));
        } else {
            // 暂存：使用Hash存储，方便快速查找（key: exam:auto:save:{examId}:{userId}）
            String hashKey = "exam:auto:save:" + dto.getExamId() + ":" + testerExam.getStudentId();
            redisTemplate.opsForValue()
                    .set(hashKey, JSONUtil.toJsonStr(msg), Duration.ofHours(2)); // 暂存数据保留2h
        }
    }

    /**
     * 暂存考试答案到Redis
     * @param dto 考试提交DTO
     * @param userId 用户ID
     */
    public void autoSaveExam(ExamSubmitDTO dto, Long userId) {
        //System.out.println("DTO:"+JSONUtil.toJsonStr(dto));
        Long examId;
        examId = dto.getExamId();
        System.out.println("examID:"+examId);
        // 效验考试状态
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId,examId)
                        .eq(TesterExam::getStudentId,userId)
        );
        if (testerExam == null||!testerExam.getStatus().equals(1)) {
            System.out.println(testerExam);
            throw new RuntimeException("考试状态异常，请联系管理员");
        }
        // 允许反复覆盖

        saveAnswers(dto, testerExam, false);
    }

    /**
     * 从Redis获取暂存的考试答案
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 暂存的考试提交DTO，如果不存在则返回null
     */
    public ExamSubmitDTO getAutoSaveExam(Long examId, Long userId) {
        // 验证考试状态
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId, examId)
                        .eq(TesterExam::getStudentId, userId)
        );
        if (testerExam == null || !testerExam.getStatus().equals(1)) {
            throw new RuntimeException("考试状态异常，请联系管理员");
        }

        // 从Redis获取暂存数据
        String hashKey = "exam:auto:save:" + examId + ":" + userId;
        String json = redisTemplate.opsForValue().get(hashKey);

        if (json == null || json.isEmpty()) {
            return null; // 没有暂存数据
        }

        try {
            // 解析Redis中的消息
            ExamSubmitMessage msg = JSONUtil.toBean(json, ExamSubmitMessage.class);
            
            // 转换为ExamSubmitDTO返回
            ExamSubmitDTO dto = new ExamSubmitDTO();
            dto.setExamId(msg.getExamId());
            dto.setSubmitTime(msg.getSubmitTime());
            dto.setAnswers(msg.getAnswers());
            
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取暂存数据失败，数据格式异常");
        }
    }

    /**
     * 交卷接口，仅存入数据库
     * @param dto 交卷DTO
     */
    public void submitExam(ExamSubmitDTO dto,Long userId) {
        Long examId = dto.getExamId();
        // System.out.println("DTO:"+JSONUtil.toJsonStr(dto));

         // 效验考试状态
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId,examId)
                        .eq(TesterExam::getStudentId,userId)
        );

        if (testerExam == null||!testerExam.getStatus().equals(1)) {
            throw new RuntimeException("考试状态异常，请联系管理员");
        }
        String key = "exam:submit:"+examId+":"+userId;

         // 使用setNx防止重复交卷 --- TTL 过期五分钟自动删除
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(10));

        if (Boolean.FALSE.equals(first)) {
            throw new RuntimeException("请勿重复交卷");
        }

       saveAnswers(dto,testerExam,true);
    }

    /**
     * 异步入库：考生提交试卷---同时拉取试题答案，判断客观题答案
     * 在5秒内轮询处理，每次批量处理一定数量的数据，Redis队列为空时立即退出
     */
    @Scheduled(fixedDelay = 5000)
    public void syncSubmit(){
        // 检查Redis队列是否有数据，如果没有数据则直接返回，避免空跑
        Long queueSize = redisTemplate.opsForList().size("exam:submit:queue");
        if (queueSize == null || queueSize == 0) {
            System.out.println("redis无数据");
            return;
        }

        // 设置处理时间窗口为5秒
        long startTime = System.currentTimeMillis();
        long timeLimit = 5000; // 5秒
        int batchSize = 20; // 每次批量处理消息数量

        // 在5秒内循环处理
        while (System.currentTimeMillis() - startTime < timeLimit) {
            // 批量处理一定数量的消息
            int processedCount = processBatch(batchSize);

            // 如果本次没有处理任何消息，说明队列已空，退出循环
            if (processedCount == 0) {
                break;
            }

            // 如果处理时间接近限制，退出循环
            if (System.currentTimeMillis() - startTime >= timeLimit - 100) {
                break;
            }
        }
    }

    /**
     * 批量处理交卷消息
     * @param batchSize 批量处理数量
     * @return 实际处理的消息数量
     */
    @Transactional
    public int processBatch(int batchSize) {
        int processedCount = 0;

        for (int i = 0; i < batchSize; i++) {
            // 从Redis队列中获取交卷消息
            String json = redisTemplate.opsForList()
                    .rightPop("exam:submit:queue");

            // 如果队列为空，退出循环
            if (json == null) {
                break;
            }

            try {
                ExamSubmitMessage msg = JSONUtil.toBean(json, ExamSubmitMessage.class);
                processSubmitMessage(msg);
                processedCount++;
            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        return processedCount;
    }

    /**
     * 处理单条交卷消息
     * @param msg 交卷消息
     */
    @Transactional
    public void processSubmitMessage(ExamSubmitMessage msg) {
        Long examId = msg.getExamId();
        Long userId = msg.getUserId();
        Long attemptId = msg.getAttemptId();

        // 查询本场考试关联的题目及其答案
        List<QuestionAnswerDTO> questionList = questionMapper.selectQuestionsWithAnswer(examId);

        // 将题目列表转换为Map，方便根据questionId快速查找
        Map<Long, QuestionAnswerDTO> questionMap = questionList.stream()
                .collect(Collectors.toMap(QuestionAnswerDTO::getQuestionId, q -> q));

        // 获取考生的作答记录（从Redis消息中获取）
        List<AnswerDTO> studentAnswers = msg.getAnswers();

        // 遍历考生的答案，进行比对和评分
        for (AnswerDTO studentAnswer : studentAnswers) {
            Long questionId = studentAnswer.getQuestionId();
            QuestionAnswerDTO question = questionMap.get(questionId);

            if (question == null) {
                continue; // 题目不存在，跳过
            }

            // 将学生答案转换为String存储
            String studentAnswerStr = convertAnswerToString(studentAnswer.getAnswer());
            String correctAnswer = question.getCorrectAnswer();

            // 创建作答记录
            AnswerRecord answerRecord = new AnswerRecord();
            answerRecord.setStudentExamId(attemptId); // 使用attemptId，不是examId
            answerRecord.setQuestionId(questionId);
            answerRecord.setStudentAnswer(studentAnswerStr);

            // 判断题目类型：5为主观题，其他为客观题
            Integer questionType = question.getQuestionType();
            if (questionType != null &&( questionType == 5 || questionType ==   4)) {
                // 主观题：不自动判分，等待教师批阅
                answerRecord.setAutoScore(0);
                answerRecord.setIsReviewed(0); // 未批阅
                answerRecord.setFinalScore(0); // 初始分数为0
            } else {
                // 客观题：自动判分
                boolean isCorrect = compareAnswers(studentAnswerStr, correctAnswer);
                Double questionScore = question.getScore();
                Integer autoScore = isCorrect ? questionScore.intValue() : 0;

                answerRecord.setAutoScore(autoScore);
                answerRecord.setIsReviewed(1); // 客观题自动批阅完成
                answerRecord.setFinalScore(autoScore); // 客观题最终分数等于自动分数
            }

            // 插入作答记录
            answerRecordMapper.insert(answerRecord);
        }

        // 更新考生考试状态为已提交
        LocalDateTime submitTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.getSubmitTime()),
                ZoneId.systemDefault()
        );

        testerExamMapper.update(
                null,
                new LambdaUpdateWrapper<TesterExam>()
                        .eq(TesterExam::getId, attemptId)
                        .set(TesterExam::getStatus, 2) // 已提交
                        .set(TesterExam::getSubmitTime, submitTime)
        );
    }

    /**
     * 将答案对象转换为String
     * @param answer 答案对象（可能是String、List或null）
     * @return 答案字符串
     */
    private String convertAnswerToString(Object answer) {
        if (answer == null) {
            return "";
        }
        if (answer instanceof String) {
            return (String) answer;
        }
        if (answer instanceof List) {
            return JSONUtil.toJsonStr(answer);
        }
        return String.valueOf(answer);
    }

    /**
     * 比对答案（支持字符串和JSON格式的列表）
     * @param studentAnswer 学生答案
     * @param correctAnswer 正确答案
     * @return 是否匹配
     */
    private boolean compareAnswers(String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || correctAnswer == null) {
            return false;
        }

        // 去除空格后比较
        String studentTrimmed = studentAnswer.trim();
        String correctTrimmed = correctAnswer.trim();

        // 直接字符串比较
        if (studentTrimmed.equals(correctTrimmed)) {
            return true;
        }

        // 尝试JSON解析后比较（处理列表答案的情况）
        try {
            Object studentObj = JSONUtil.parse(studentTrimmed);
            Object correctObj = JSONUtil.parse(correctTrimmed);
            return JSONUtil.toJsonStr(studentObj).equals(JSONUtil.toJsonStr(correctObj));
        } catch (Exception e) {
            // 解析失败，使用字符串比较
            return false;
        }
    }


    /**
     *
     * @param studentExamId 对应考试Id
     * @param questionId 问题Id
     * @param teacherScore 教师评分
     */
    @Transactional
    public void reviewOneQuestion(Long studentExamId,
                                  Long questionId,
                                  Double teacherScore) {

        answerRecordMapper.update(
                null,
                new LambdaUpdateWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getStudentExamId, studentExamId)
                        .eq(AnswerRecord::getQuestionId, questionId)
                        .set(AnswerRecord::getTeacherScore, teacherScore)
                        .setSql("final_score = teacher_score ")
                        .set(AnswerRecord::getIsReviewed, 1)
        );
    }

    @Transactional
    public void finishReview(Long studentExamId) {

        // 1. 是否还有未批阅题目
        Long unReviewedCount = answerRecordMapper.selectCount(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getStudentExamId, studentExamId)
                        .eq(AnswerRecord::getIsReviewed, 0)
        );

        if (unReviewedCount > 0) {
            return; // 还有题没批，不动状态
        }

        // 2. 汇总总分
        Double totalScore = answerRecordMapper.sumFinalScore(studentExamId);

        // 3. 更新答卷
        testerExamMapper.update(
                null,
                new LambdaUpdateWrapper<TesterExam>()
                        .eq(TesterExam::getId, studentExamId)
                        .set(TesterExam::getTotalScore, totalScore)
                        .set(TesterExam::getStatus, 2)
        );
    }


    @Override
    public ExamPaperDetailVO getPaperDetailForCreator(Long examId, Long creatorId) {
        if (examId == null) {
            throw new IllegalArgumentException("考试ID不能为空");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("创建者ID不能为空");
        }

        Exam exam = examMapper.selectById(examId);
        if (exam == null || (exam.getIsDeleted() != null && exam.getIsDeleted() == 1)) {
            throw new IllegalArgumentException("考试不存在");
        }
        if (exam.getCreatorId() == null || !exam.getCreatorId().equals(creatorId)) {
            throw new IllegalArgumentException("无权限查看该试卷");
        }

        List<QuestionSimpleInfoVO> questions = questionMapper.getQuestions(examId);
        List<PaperQuestionDetailVO> detailList = new ArrayList<>();
        if (questions != null) {
            for (QuestionSimpleInfoVO q : questions) {
                PaperQuestionDetailVO item = new PaperQuestionDetailVO();
                item.setQuestion(q);
                if (q != null && q.getQuestionId() != null) {
                    var qa = questionAnswerMapper.selectByQuestionId(q.getQuestionId());
                    if (qa != null) {
                        item.setCorrectAnswer(qa.getCorrectAnswer());
                        item.setAnswerAnalysis(qa.getAnswerAnalysis());
                    }
                }
                detailList.add(item);
            }
        }

        ExamPaperDetailVO vo = new ExamPaperDetailVO();
        vo.setExamId(exam.getId());
        vo.setExamName(exam.getExamName());
        vo.setExamCode(exam.getExamCode());
        vo.setDescription(exam.getDescription());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setLimitMinutes(exam.getLimitMinutes());
        vo.setStatus(exam.getStatus());
        vo.setShowAnswers(exam.getPaperShow());
        vo.setQuestions(detailList);
        return vo;
    }
}
