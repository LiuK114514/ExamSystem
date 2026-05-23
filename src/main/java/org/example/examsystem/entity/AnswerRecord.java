package org.example.examsystem.entity;
import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 学生答案表
 */
@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联学生参与的考试ID
    private Long studentExamId;

    // 关联相关问题ID
    private Long questionId;

    private String studentAnswer;

    // 客观题系统自动识别判分
    private Double autoScore;

    // 主观题教师评分
    private Double teacherScore;

    // 是否已批阅 （0：未批 1：已批）
    private Integer isReviewed;

    private Double finalScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
    @TableField
    private String aiReason; // AI评分理由
}
