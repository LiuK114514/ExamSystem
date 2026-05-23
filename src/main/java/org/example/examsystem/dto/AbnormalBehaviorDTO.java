package org.example.examsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异常数据DTO
 */
@Data
public class AbnormalBehaviorDTO {
    private String behaviorType;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime occurTime;
}
