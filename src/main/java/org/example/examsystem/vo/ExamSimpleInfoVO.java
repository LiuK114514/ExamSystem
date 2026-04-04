package org.example.examsystem.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试基本信息VO
 */
@Data
public class ExamSimpleInfoVO {
    private Long examId;
    private String examName;
    private String description;
    private Integer limitMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer examCode;
    private String realName;
    private Integer status;
    private Boolean paperShow;
}
