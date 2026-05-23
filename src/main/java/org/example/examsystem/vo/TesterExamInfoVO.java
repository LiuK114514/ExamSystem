package org.example.examsystem.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 参与者考试信息VO
 */
@Data
public class TesterExamInfoVO {
    private Long testerExamId;      // tester_exam表ID
    private Long examId;            // exam表ID
    private String examName;        // 考试名称
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime examStartTime;     // 考试开始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime examEndTime;       // 考试结束时间
    private Integer limitMinutes;   // 限时（分钟）
    private Integer examStatus;     // 考试状态
    private Integer testerStatus;   // 学生参与考试状态
    private Double totalScore;     // 学生总分
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime testerStartTime;   // 学生开始考试时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime testerSubmitTime;  // 学生提交考试时间
    private Integer paperShow;
    private Integer reviewStatus;
}
