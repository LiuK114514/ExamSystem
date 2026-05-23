package org.example.examsystem.vo;

import lombok.Data;

/**
 * 阅卷详细信息（主观题） VO
 */
@Data
public class TeacherReviewQuestionDetailVO {

    private Long examId;           // 考试 ID
    private Long questionId;       // 题目 ID
    private Long studentId;        // 学生 ID

    // 题目相关
    private Integer questionType;  // 必须 = 5（主观题）
    private String content;        // 题干（富文本/图片等）

    // 标准答案（主观题可选，有的系统无标准答案）
    private String correctAnswer;

    // 学生作答
    private String studentAnswer;

    // 该题分值（来自 exam_question）
    private Integer score;

    // 初始自动分（主观题一般为 0）
    private Double teacherScore;

    private Integer reviewed;      // 是否已批阅（来自 answer_record.is_reviewed）
    //ai评分理由
    private String aiReason;
}

