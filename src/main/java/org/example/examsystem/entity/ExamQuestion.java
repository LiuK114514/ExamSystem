package org.example.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 考试与题目关联表
 */
@Data
@TableName("exam_question")
public class ExamQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的考试ID
    private Long examId;

    // 试题ID
    private Long questionId;

    // 本题在本次考试的分值
    private Double score;

    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    private String answerAnalysis;
}