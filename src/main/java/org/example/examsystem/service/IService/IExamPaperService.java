package org.example.examsystem.service.IService;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.examsystem.entity.AnswerRecord;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.vo.ProgressVO;
import org.example.examsystem.vo.ExamPaperDetailVO;
import org.example.examsystem.vo.QuestionDetailVO;
import org.example.examsystem.vo.TeacherReviewQuestionDetailVO;

import java.util.List;

public interface IExamPaperService extends IService<AnswerRecord> {

    List<QuestionDetailVO>  getQuestionDetail(Long examId,Long userId);

    List<Long> getQuestionIdByExamId(Long examId);

    TeacherReviewQuestionDetailVO getReviewQuestionDetail(Long examId, Long questionId, Long studentId);

    List<Long> getSubjectiveQuestionIds(Long examId);

    Boolean judge(Long examId,Long questionId,Long studentId,Integer score);

    ProgressVO getReviewProgress(Long examId);

    /**
     * 出题者获取试卷详细（考试基本信息 + 题目列表（含选项、分值、标准答案、解析））
     */
    ExamPaperDetailVO getPaperDetailForCreator(Long examId, Long creatorId);

    int reviewOneQuestion(Long studentExamId,
                           Long questionId,
                           Double teacherScore);

    void finishReview(Long studentExamId);

}
