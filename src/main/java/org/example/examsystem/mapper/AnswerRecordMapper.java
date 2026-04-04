package org.example.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.examsystem.dto.StudentReviewCount;
import org.example.examsystem.entity.AnswerRecord;
import org.example.examsystem.vo.QuestionDetailVO;
import org.example.examsystem.vo.TeacherReviewQuestionDetailVO;

import java.util.List;

public interface AnswerRecordMapper extends BaseMapper<AnswerRecord> {

    List<QuestionDetailVO> getQuestionDetails(@Param("examId") Long examId, @Param("userId") Long userId);

    TeacherReviewQuestionDetailVO getTeacherReviewDetail(
            @Param("examId") Long examId,
            @Param("questionId") Long questionId,
            @Param("studentId") Long studentId
    );

    Integer countSubjectiveQuestions(@Param("examId") Long examId);

    List<StudentReviewCount> countReviewedPerStudent(@Param("examId")Long examId);

    @Select("""
    SELECT IFNULL(SUM(final_score),0)
    FROM answer_record
    WHERE student_exam_id = #{studentExamId}
      AND is_deleted = 0
""")
    Double sumFinalScore(@Param("studentExamId") Long studentExamId);
}

