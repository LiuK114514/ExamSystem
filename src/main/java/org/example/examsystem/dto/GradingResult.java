package org.example.examsystem.dto;

import lombok.Data;

@Data
public class GradingResult {
    private Double score;
    private Integer isReviewed; // 1已批阅 0待人工
    private String reason;

    public GradingResult(Double score, Integer isReviewed, String reason) {
        this.score = score;
        this.isReviewed = isReviewed;
        this.reason = reason;
    }

    public static GradingResult auto(double score) {
        return new GradingResult(score, 1, null);
    }

    public static GradingResult ai(double score, String reason) {
        return new GradingResult(score, 0, reason); // 待复核
    }

    public static GradingResult manual() {
        return new GradingResult(0.0, 0, null);
    }

    public boolean isManual() {
        return isReviewed == 0;
    }
}
