package org.example.examsystem.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.examsystem.dto.CreateExamRequest;
import org.example.examsystem.dto.QuestionInExamDTO;
import org.example.examsystem.dto.UpdateExamBasicInfoRequest;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.Question;
import org.example.examsystem.entity.QuestionOption;
import org.example.examsystem.entity.QuestionAnswer;
import org.example.examsystem.entity.ExamQuestion;
import org.example.examsystem.mapper.*;
import org.example.examsystem.service.IService.IExamService;
import org.example.examsystem.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl extends ServiceImpl<ExamMapper,Exam> implements IExamService {

    private final ExamMapper examMapper;
    private final TesterExamMapper testerExamMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionMapper questionMapper;
    private final QuestionAnswerMapper questionAnswerMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ExamQuestionMapper examQuestionMapper;




    /**
     * 用户获取自己 参加过的考试
     * @param userId 用户Id
     * @return 考试列表
     */
    @Override
    public Page<?> getTesterExams(Long userId, Integer page, Integer pageSize) {
        // 参与者查询
            Page<TesterExamInfoVO> examPage = new Page<>(page,pageSize);
            return testerExamMapper.getExamsByTesterId(examPage,userId);
    }

    /**
     * 用户获取自己 发布过的考试
     * @param userId 用户Id
     * @return 考试列表
     */
    @Override
    public Page<?> getCreatorExams(Long userId, Integer page, Integer pageSize) {
        Page<CreatorExamInfoVO> pages = testerExamMapper.getExamsByCreatorId(
                new Page<>(page, pageSize), userId);
        for(CreatorExamInfoVO vo : pages.getRecords()){
            List<QuestionDetailVO> questionDetailVOS = answerRecordMapper.getQuestionDetails(vo.getExamId(),userId);
            vo.setQuestionDetailVOList(questionDetailVOS);
        }
        return pages;
    }

    /**
     * 查询某一考试下的考试参加人数（出题者）
     * @param examId 考试Id
     * @return 参与人数
     */
    @Override
    public Long getAllCount(Long examId) {
        return testerExamMapper.selectCount(
                Wrappers.<TesterExam>lambdaQuery()
                        .eq(TesterExam::getExamId,examId)
                        .eq(TesterExam::getIsDeleted,0)
        );
    }

    /**
     * 获取考生在本场考试的排名情况
     * @param examId 考试Id
     * @param userId 考试Id
     * @return rank、total
     */
    @Override
    public RankInfoVO getMyRank(Long examId, Long userId) {
        return testerExamMapper.getRank(examId,userId);
    }

    /**
     * 获取参与本场考试的所有参与者
     * @param examId 用户ID
     * @param page 当前页
     * @param pageSize 页尺寸
     * @return 参与者链表
     */
    @Override
    public Page<UserSimpleInfoVO> getAllTesters(Long examId, Integer page, Integer pageSize) {
        Page<UserSimpleInfoVO> pageVO = new Page<>(page, pageSize);
        return testerExamMapper.getAllTestersByPage(pageVO,examId);
    }

    /**
     * 根据考试Id查询题目链表
     * @param examId 考试Id
     * @return 试题链表
     */
    @Override
    public List<QuestionSimpleInfoVO> getQuestions(Long examId) {
        return questionMapper.getQuestions(examId);
    }

    /**
     * 创建考试并绑定题目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createExam(CreateExamRequest request) {
        if (!StringUtils.hasText(request.getExamName())) {
            throw new IllegalArgumentException("考试名称不能为空");
        }
        if (request.getExamName().length() > 50) {
            throw new IllegalArgumentException("考试名称不超过50个字符");
        }
        if (!StringUtils.hasText(request.getExamCode())) {
            throw new IllegalArgumentException("考试码不能为空");
        }
        if (request.getCreatorId() == null) {
            throw new IllegalArgumentException("创建者ID不能为空");
        }
        if (request.getDuration() == null || request.getDuration() <= 0) {
            throw new IllegalArgumentException("考试时长必须大于0");
        }
        if (request.getDuration() > 600) {
            throw new IllegalArgumentException("考试时长不能超过600分钟");
        }
        Integer examCodeInt;
        try {
            examCodeInt = Integer.valueOf(request.getExamCode());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("考试码必须为六位数字");
        }
        if (request.getExamCode().length() != 6) {
            throw new IllegalArgumentException("考试码必须为六位数字");
        }

        LocalDateTime start;
        try {
            start = LocalDateTime.of(LocalDate.parse(request.getStartDate()), LocalTime.parse(request.getStartTime()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("开始时间格式错误，应为 yyyy-MM-dd 与 HH:mm:ss");
        }

        // 唯一性：考试码不能重复
        Long exists = examMapper.selectCount(Wrappers.<Exam>lambdaQuery()
                .eq(Exam::getExamCode, examCodeInt)
                .eq(Exam::getIsDeleted, 0));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("考试码已存在");
        }

        // 创建试卷
        Exam exam = new Exam();
        exam.setExamName(request.getExamName());
        exam.setExamCode(examCodeInt);
        exam.setCreatorId(request.getCreatorId());
        exam.setDescription(request.getDescription());
        exam.setLimitMinutes(request.getDuration());
        exam.setPaperShow(Boolean.TRUE.equals(request.getShowAnswers()));
        exam.setStartTime(start);
        exam.setEndTime(start.plusMinutes(request.getDuration()));
        exam.setStatus(0);
        examMapper.insert(exam);

        Long examId = exam.getId();

        // 如果提供了题目列表，则创建所有题目
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int sort = 1;
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuestionInExamDTO questionDTO = request.getQuestions().get(i);
                int questionIndex = i + 1;

                // 验证题目信息
                if (questionDTO.getQuestionType() == null || questionDTO.getQuestionType().trim().isEmpty()) {
                    throw new IllegalArgumentException(String.format("第%d道题目的题型(questionType)不能为空", questionIndex));
                }
                if (!StringUtils.hasText(questionDTO.getContent())) {
                    throw new IllegalArgumentException(String.format("第%d道题目的题干(content)不能为空", questionIndex));
                }
                if (questionDTO.getScore() == null || questionDTO.getScore() <= 0) {
                    throw new IllegalArgumentException(String.format("第%d道题目的分数(score)必须大于0", questionIndex));
                }

                // 调试日志：检查 options 字段
                log.info("第{}道题目 - options值: {}, 是否为null: {}, 是否为空: {}",
                    questionIndex,
                    questionDTO.getOptions(),
                    questionDTO.getOptions() == null,
                    questionDTO.getOptions() != null && questionDTO.getOptions().isEmpty());

                // 转换题型：支持字符串和数字两种格式
                int questionType;
                String typeStr = questionDTO.getQuestionType().trim().toLowerCase();
                try {
                    // 先尝试作为数字解析
                    questionType = Integer.parseInt(typeStr);
                } catch (NumberFormatException ex) {
                    // 如果不是数字，尝试字符串映射
                    switch (typeStr) {
                        case "single":
                        case "单选题":
                            questionType = 1;
                            break;
                        case "multiple":
                        case "多选题":
                            questionType = 2;
                            break;
                        case "judge":
                        case "判断题":
                            questionType = 3;
                            break;
                        case "fill":
                        case "填空题":
                            questionType = 4;
                            break;
                        case "essay":
                        case "简答题":
                        case "主观题":
                            questionType = 5;
                            break;
                        default:
                            throw new IllegalArgumentException(String.format("第%d道题目的题型不支持，支持的类型：single/multiple/judge/fill/essay 或 1/2/3/4/5", questionIndex));
                    }
                }
                if (questionType < 1 || questionType > 5) {
                    throw new IllegalArgumentException(String.format("第%d道题目的题型必须在1-5之间", questionIndex));
                }

                // 创建题目实体
                Question question = new Question();
                question.setQuestionType(questionType);
                question.setContent(questionDTO.getContent());
                question.setCreatorId(request.getCreatorId());
                questionMapper.insert(question);
                Long questionId = question.getId();

                // 创建选项（仅选择/判断题需要）- 直接使用 List<String>
                if (questionType == 1 || questionType == 2) {
                    List<String> optionsList = questionDTO.getOptions();
                    log.info("第{}道题目 - 题型: {}, options: {}, 是否为null: {}",
                        questionIndex, questionType, optionsList, optionsList == null);

                    if (optionsList == null || optionsList.isEmpty()) {
                        throw new IllegalArgumentException(String.format("第%d道题目（题型: %d）需要提供选项(options/opinions字段)", questionIndex, questionType));
                    }
                    char key = 'A';
                    for (String opt : questionDTO.getOptions()) {
                        if (StringUtils.hasText(opt)) {
                            QuestionOption qo = new QuestionOption();
                            qo.setQuestionId(questionId);
                            qo.setOptionKey(String.valueOf(key));
                            qo.setOptionText(opt.trim());
                            questionOptionMapper.insert(qo);
                            key++;
                        }
                    }
                }

                // 设置标准答案（如果有）
                if (StringUtils.hasText(questionDTO.getAnswer())) {
                    QuestionAnswer qa = new QuestionAnswer();
                    qa.setQuestionId(questionId);
                    qa.setCorrectAnswer(questionDTO.getAnswer());
                    qa.setAnswerAnalysis(questionDTO.getAnswerAnalysis());
                    questionAnswerMapper.insert(qa);
                }
// 在创建ExamQuestion对象后添加分数设置
                ExamQuestion examQuestion = new ExamQuestion();
                examQuestion.setExamId(examId);
                examQuestion.setQuestionId(questionId);
// 将Integer分数转换为Double
                examQuestion.setScore(questionDTO.getScore().doubleValue());
                examQuestion.setSort(sort++);
                examQuestion.setIsDeleted(0);
                examQuestionMapper.insert(examQuestion);

            }
        }

        // 获取该试卷的题目列表（包含刚创建的题目）
        List<QuestionSimpleInfoVO> questions = questionMapper.getQuestions(examId);

        Map<String, Object> data = new HashMap<>();
        data.put("examId", examId);
        data.put("examCode", exam.getExamCode());
        data.put("questions", questions);
        return data;
    }

    @Override
    public void updateExamBasicInfo(Long examId, Long creatorId, UpdateExamBasicInfoRequest request) {
        // TODO: 实现更新考试基本信息逻辑
    }

    @Override
    public void completeExamEdit(Long examId, Long creatorId) {
        // TODO: 实现完成试卷编辑验证逻辑
    }

    /**
     * 获取成绩基本信息
     * @param examId 考试ID
     * @return Map
     */
    public Map<String, Object> getBasicStats(Long examId) {
        return testerExamMapper.getBasicStats(examId);
    }

    /**
     * 分数段统计
     * @param examId 考试ID
     * @return List
     */
    public List<Map<String, Object>> getScoreRanges(Long examId) {
        List<Map<String, Integer>> ranges = List.of(
                Map.of("start", 0, "end", 59),
                Map.of("start", 60, "end", 69),
                Map.of("start", 70, "end", 79),
                Map.of("start", 80, "end", 89),
                Map.of("start", 90, "end", 100)
        );

        return testerExamMapper.getScoreRanges(examId, ranges);
    }
}
