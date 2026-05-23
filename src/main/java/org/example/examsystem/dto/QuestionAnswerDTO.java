package org.example.examsystem.dto;

import lombok.Data;

/**
 * 考试判题DTO
 */
@Data
public class QuestionAnswerDTO {
    private Long questionId;
    private String content;
    private Integer questionType;
    private String correctAnswer;
    private Double score;
}

