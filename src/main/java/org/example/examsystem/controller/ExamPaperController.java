package org.example.examsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.examsystem.anno.Log;
import org.example.examsystem.annotation.LoginUser;
import org.example.examsystem.dto.JudgeRequest;
import org.example.examsystem.dto.QuestionScoreDTO;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.TesterExamMapper;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.service.IService.IExamPaperService;
import org.example.examsystem.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试卷相关模块controller
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/exam/paper")
public class ExamPaperController {

    private final IExamPaperService examPaperService;
    private final TesterExamMapper testerExamMapper;
    private final UserMapper userMapper;
    private final ExamMapper examMapper;

    /**
     * 获取本次考试题目Id列表
     * @param examId 考试Id
     * @return 题目Id列表
     */
    @GetMapping("/{examId}/questions")
    public Result getQuestionIds(@PathVariable("examId") Long examId){
        return Result.ok(examPaperService.getQuestionIdByExamId(examId));
    }

    /**
     * 获取主观题ID列表，即需要批阅的题目
     * @param examId 考试ID
     * @return Id列表
     */
    @GetMapping("/{examId}/questions/subjective")
    public Result getSubjectiveQuestionIds(@PathVariable("examId") Long examId){
        return Result.ok(examPaperService.getSubjectiveQuestionIds(examId));
    }

    /**
     * 查询考生的某题的具体答题情况
     * @param examId 考试Id
     * @param questionId 试题Id
     * @param userId 用户Id
     * @return 某一题答题详细
     */
    @GetMapping("/{examId}/questions/{questionId}/details")
    public Result getQuestionDetails(@PathVariable("examId") Long examId,
                                     @PathVariable("questionId") Long questionId,
                                     @LoginUser Long userId
                                     ){
        return Result.ok(examPaperService.getReviewQuestionDetail(examId,questionId,userId));
    }

    /**
     * 评卷接口
     * @param examId 考试ID
     * @param judgeRequest 评卷DTO
     * @param userId 当前登录用户ID
     * @return 评卷成功
     */
    @Log(module = "试卷管理", operationType = "评卷", description = "批阅试卷")
    @PostMapping("/{examId}/judge")
    public Result judgeQuestionAnswer(@PathVariable("examId") Long examId,
                                      @RequestBody JudgeRequest judgeRequest,
                                      @LoginUser Long userId){
        if(userId == null){
            return Result.info(401, "未登录或Token无效");
        }

        User currentUser = userMapper.selectById(userId);
        if(currentUser == null){
            return Result.info(401, "用户不存在");
        }

        Exam exam = examMapper.selectById(examId);
        if(exam == null){
            return Result.info(404, "考试不存在");
        }

        if(!exam.getCreatorId().equals(userId)){
            return Result.info(403, "只有发卷人才能进行评卷操作");
        }

        Long testerId = judgeRequest.getTesterId();
        TesterExam result = testerExamMapper.selectOne(
                new LambdaQueryWrapper<TesterExam>()
                        .eq(TesterExam::getExamId,examId)
                        .eq(TesterExam::getStudentId,testerId)
        );
        if(result==null){
            return Result.info(404,"未找到对应考试");
        }
        Long testerExamId = result.getId();
        List<QuestionScoreDTO> questions = judgeRequest.getQuestions();
        
        if(questions == null || questions.isEmpty()){
            return Result.info(500, "评卷题目列表不能为空");
        }
        
        for(QuestionScoreDTO q : questions){
            if(q.getQuestionId() == null){
                return Result.info(500, "题目ID不能为空");
            }
            Double score = q.getUserScore();
            if(score == null){
                return Result.info(500, "分数不能为空");
            }
            if(score < 0){
                return Result.info(500, "分数不能为负数");
            }
            
            int updatedRows = examPaperService.reviewOneQuestion(
                    testerExamId,
                    q.getQuestionId(),
                    score
            );
            if(updatedRows == 0){
                return Result.info(404, "题目不存在或不属于本场考试");
            }
        }

        examPaperService.finishReview(testerExamId);
        return Result.ok("批阅成功");
    }


    /**
     * 获取本次考试批阅进度
     * @param examId 考试ID
     * @return 进度
     */
    @GetMapping("/{examId}/progress")
    public Result getReviewProgress(@PathVariable("examId") Long examId){
        return Result.ok(examPaperService.getReviewProgress(examId));
    }
}
