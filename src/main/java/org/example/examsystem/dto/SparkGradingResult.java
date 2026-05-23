package org.example.examsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SparkGradingResult {
    private boolean success;
    private Double score;
    private String reason;
    public SparkGradingResult(){
        success = false;
        score = 0.0;
        reason = "";
    }
    public SparkGradingResult(boolean success, Double score, String reason) {
        this.success = success;
        this.score = score;
        this.reason = reason;
    }
    public static SparkGradingResult success(double score, String reason) {
        return SparkGradingResult.builder()
                .success(true).score(score).reason(reason).build();
    }

    public static SparkGradingResult failed() {
        return SparkGradingResult.builder()
                .success(false).score(null).reason(null).build();
    }
}
