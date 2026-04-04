package org.example.examsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.ibatis.annotations.Param;
import org.example.examsystem.anno.Log;
import org.example.examsystem.annotation.LoginUser;
import org.example.examsystem.dto.AbnormalBehaviorDTO;
import org.example.examsystem.dto.AnswerDTO;
import org.example.examsystem.dto.ExamSubmitDTO;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.AnswerRecordMapper;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.TesterExamMapper;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.service.IService.IExamPaperService;
import org.example.examsystem.service.IService.IExamService;
import org.example.examsystem.service.IService.IGradeService;
import org.example.examsystem.service.Impl.AbnormalBehaviorServiceImpl;
import org.example.examsystem.service.Impl.ExamPaperServiceImpl;
import org.example.examsystem.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考试相关模块controller
 */
@Slf4j
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final IExamService examService;
    private final ExamMapper examMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final IGradeService gradeService;
    private final TesterExamMapper testerExamMapper;
    private final AbnormalBehaviorServiceImpl abnormalBehaviorService;
    private final ExamPaperServiceImpl examPaperService;
    private final UserMapper userMapper;

    /**
     * 分页查询参加本场考试人员信息
     * @param examId 本场考试Id
     * @param page 当前页
     * @param size 页尺寸
     * @return 人员信息VO类
     */
    @GetMapping("/{examId}/testers")
    public Result getAllTest(
            @PathVariable("examId") long examId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size){
        return Result.ok(examService.getAllTesters(examId,page,size));
    }

    /**
     * 用户获取考试信息
     * 考生获取参加过的考试
     * @return exam记录分页
     */
    @PostMapping("/my-exam/tester")
    public Result getTesterExams(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size
    ){
        if(examService.getTesterExams(userId,page,size)!=null){
            return Result.ok(examService.getTesterExams(userId,page,size));
        }else{
            return Result.fail("错误参数");
        }
    }

    @GetMapping("/my-exam/creator")
    public Result getCreatorExam(@LoginUser Long userId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "100") int size){
        val result = examService.getCreatorExams(userId, page, size);

        if(result!=null){
            return Result.ok(result);
        }else{
            return Result.fail("错误参数");
        }
    }

    /**
     * 获取本场考试详细信息，即整卷浏览
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 题目及其作答情况
     */
    @GetMapping("my-exam/details/{examId}")
    public Result getMyExamDetails(
            @PathVariable("examId") Long examId,
            @LoginUser Long userId
    ){
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId, examId)
                        .eq(TesterExam::getStudentId, userId)
        );
        if(testerExam==null){
            return Result.info(404,"未找到该考试记录");
        }
        List<QuestionDetailVO> questions = answerRecordMapper.getQuestionDetails(examId,userId);
        Exam exam = examMapper.selectById(examId);
        ExamDetailVO examDetailVO = new ExamDetailVO(exam,questions);

        return Result.ok(examDetailVO);
    }

    /**
     * 用户输入考试码加入考试 （创建关联表）
     * 并获取题目
     * @return 题目链表
     */
    @GetMapping("/check")
    public Result checkExam(
            @LoginUser Long userId,
            @RequestParam String code
    ){
        if(code.length()!=6){
            return Result.fail("考试码必须为6位");
        }
        // 查询考试
        LambdaQueryWrapper<Exam>  queryWrapper = new LambdaQueryWrapper<>();
        try{
            queryWrapper.eq(Exam::getExamCode,Integer.valueOf(code));
        }catch (NumberFormatException ex){
            return Result.fail("考试码必须为6位数字");
        }
        Exam exam =  examMapper.selectOne(queryWrapper);
        if(exam==null){
            return Result.info(404,"未找到该考试！");
        }
        User creator = userMapper.selectById(exam.getCreatorId());
        ExamSimpleInfoVO examSimpleInfoVO = new ExamSimpleInfoVO();
        BeanUtils.copyProperties(exam,examSimpleInfoVO);
        BeanUtils.copyProperties(creator,examSimpleInfoVO);
        examSimpleInfoVO.setExamId(exam.getId());
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>().eq(TesterExam::getExamId, exam.getId())
                        .eq(TesterExam::getStudentId, userId)
        );

        int status;
        if(LocalDateTime.now().isBefore(exam.getStartTime()))
            status=0;
        else if(testerExam==null||testerExam.getStatus()==1)
            status=1;
        else
            status=2;

        examSimpleInfoVO.setStatus(status);
        return Result.ok(examSimpleInfoVO);
    }

    /**
     * 点击确认后开始考试
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 题目链表
     */
    @Log(module = "考试管理", operationType = "开始考试", description = "考生开始考试")
    @Transactional
    @GetMapping("/{examId}/start")
    public Result  startExam(
            @PathVariable("examId") Long examId,
            @LoginUser Long userId
    ){
        // 首先先插考试记录，看考生是首次还是多次进入考试
        TesterExam testerExam = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId, examId)
                        .eq(TesterExam::getStudentId, userId)
        );
        Map<Long, Object> answerMap = new HashMap<>();

        if (testerExam != null) {
            System.out.println("非首次");
            // 非首次进入，取暂存
            ExamSubmitDTO autoSave = examPaperService.getAutoSaveExam(examId, userId);
            if (autoSave != null && autoSave.getAnswers() != null) {
                for (AnswerDTO a : autoSave.getAnswers()) {
                    answerMap.put(a.getQuestionId(), a.getAnswer());
                }
            }
        } else {
            // 首次进入，创建考试记录
            // 加入考试后创建考试信息关联表
            System.out.println("首次");
            testerExam = new TesterExam();
            testerExam.setExamId(examId);
            testerExam.setStudentId(userId);
            testerExam.setStartTime(LocalDateTime.now());
            testerExam.setStatus(1);

            if (testerExamMapper.insert(testerExam) == 0) {
                return Result.fail("开始考试异常，请联系管理员");
            }
        }

        // 获取考试题目信息
        List<QuestionSimpleInfoVO> questions = examService.getQuestions(examId);
        for (QuestionSimpleInfoVO q : questions) {
            if (answerMap.containsKey(q.getQuestionId())) {
                q.setUserAnswer(answerMap.get(q.getQuestionId()));
            }
        }

        return Result.ok(questions);
    }

    /**
     * 返回考试排名情况
     * @param examId 考试Id
     * @param userId 考生Id
     * @return RankInfoVO
     */
    @GetMapping("/rank/{examId}")
    public Result getRank(@PathVariable("examId") Long examId,@LoginUser Long userId){
        return Result.ok(examService.getMyRank(examId,userId));
    }

    /**
     * 出题者查询本次考试考生成绩情况
     * @param examId 考试Id
     * @return 成绩列表
     */
    @GetMapping("/{examId}/grades")
    public Result getTesterGrades(@PathVariable("examId") Long examId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size){
        return Result.ok(gradeService.getGrades(examId,page,size));
    }

    /**
     * 获取本场考试基本的基本统计数据
     * @param examId 考试Id
     * @return 数据
     */
    @GetMapping("/{examId}/distribution")
    public Map<String, Object> getBasic(@PathVariable Long examId) {
        return examService.getBasicStats(examId);
    }

    /**
     * 获取本场考试各个分数段人数
     * @param examId 考试Id
     * @return 分数段及其人数 Map
     */
    @GetMapping("/{examId}/ranges")
    public List<Map<String, Object>> getRanges(@PathVariable Long examId) {
        return examService.getScoreRanges(examId);
    }

    /**
     * 提交异常记录
     * @param examId 考试ID
     * @param userId 用户ID
     */
    @Log(module = "考试管理", operationType = "提交异常", description = "提交考试异常行为记录")
    @PostMapping("/{examId}/submit/abnormal")
    public Result submitAbnormal(@PathVariable("examId") Long examId,
                                 @LoginUser Long userId,
                                 @RequestBody List<AbnormalBehaviorDTO> list){
        int result = abnormalBehaviorService.reportBehavior(userId,examId,list);
        return Result.ok(result);
    }

    /**
     * 考生交卷

     * @param dto 交卷DTO
     */
    @Log(module = "考试管理", operationType = "交卷", description = "考生提交试卷")
    @PostMapping("/submit/paper")
    public Result submitPaper(
            @RequestBody ExamSubmitDTO dto,
            @LoginUser Long userId){
        // 先调用存Redis接口

        examPaperService.submitExam(dto,userId);
        return Result.ok("交卷成功");
        // 之后交给定时任务
    }

    /**
     * 暂存考试答案
     * @param dto 考试提交DTO
     * @param userId 用户ID
     * @return 结果
     */
    @Log(module = "考试管理", operationType = "暂存答案", description = "暂存考试答案")
    @PostMapping("/auto-save")
    public Result autoSaveExam(@RequestBody ExamSubmitDTO dto,
                               @LoginUser Long userId) {
        try {
            examPaperService.autoSaveExam(dto, userId);
            log.info("暂存成功");
            return Result.ok("暂存成功");
        } catch (RuntimeException e) {
            log.warn("暂存失败");
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取暂存的考试答案
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 暂存的考试答案，如果不存在则返回null
     */
    @GetMapping("/auto-save/{examId}")
    public Result getAutoSaveExam(@PathVariable("examId") Long examId,
                                  @LoginUser Long userId) {
        try {
            TesterExam testerExam = testerExamMapper.selectOne(
                    new LambdaQueryWrapper<TesterExam>().eq(TesterExam::getExamId, examId)
                    .eq(TesterExam::getStudentId, userId)
            );
            ExamSubmitDTO dto = examPaperService.getAutoSaveExam(examId, userId);

            if (dto == null) {
                return Result.info(404, "暂无暂存数据");
            }

            LocalDateTime startTime=testerExam.getStartTime();
            long milli = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            dto.setStartTime(milli);
            return Result.ok(dto);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
