package org.example.examsystem.utils;

import org.example.examsystem.dto.GradingResult;
import org.example.examsystem.dto.QuestionAnswerDTO;
import org.example.examsystem.dto.SparkGradingResult;
import org.example.examsystem.service.Impl.SparkAiGradingService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class GradingUtils {
    public static GradingResult grade(
            QuestionAnswerDTO question,
            String studentAnswer,
            SparkAiGradingService aiService
    ) {

        Integer type = question.getQuestionType();
        String correct = question.getCorrectAnswer();
        double score = question.getScore();
        // 空答案统一处理
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return GradingResult.auto(0);
        }

        studentAnswer = studentAnswer.trim();

        return switch (type) {
            case 1, 3 ->//判断
                    GradingResult.auto(objective(studentAnswer, correct, score));
            case 2 ->//多选
                    GradingResult.auto(multiChoice(studentAnswer, correct, score));
            case 4 ->//填空
                    fillBlank(question, studentAnswer, aiService);
            case 5 ->//主观
                    subjective(question, studentAnswer, aiService);
            default -> GradingResult.auto(0);
        };
    }

    private static GradingResult auto(double score) {
        return GradingResult.auto(score);
    }

    // ================= 客观题 =================

    private static double objective(String student, String correct, double score) {
        return student.equals(correct) ? score : 0;
    }

    // ================= 多选题=================
    private static double multiChoice(String student, String correct, double fullScore) {
        Set<String> correctSet = Arrays.stream(correct.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> studentSet = Arrays.stream(student.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        // 含错误选项 → 0分
        if (!correctSet.containsAll(studentSet)) {
            return 0;
        }

        // 全对
        if (studentSet.equals(correctSet)) {
            return fullScore;
        }

        // 漏选 → 半分
        return fullScore / 2;
    }

    // ================= 填空题 =================

    private static GradingResult fillBlank(
            QuestionAnswerDTO question,
            String student,
            SparkAiGradingService aiService
    ) {
        try {
            SparkGradingResult ai = aiService.gradeSubjectiveAnswer(
                    question.getContent(),
                    question.getCorrectAnswer(),
                    student,
                    question.getScore()
            );
            if (ai.isSuccess()) {
                return GradingResult.ai(ai.getScore(), ai.getReason());
            }
        } catch (Exception e) {
            // AI异常降级
        }
        return GradingResult.manual();
    }

    // ================= 主观题=================

    private static GradingResult subjective(
            QuestionAnswerDTO question,
            String student,
            SparkAiGradingService aiService
    ) {
        try {
            SparkGradingResult ai = aiService.gradeSubjectiveAnswer(
                    question.getContent(),
                    question.getCorrectAnswer(),
                    student,
                    question.getScore()
            );

            if (ai.isSuccess()) {
                return GradingResult.ai(ai.getScore(), ai.getReason());
            }

        } catch (Exception e) {
            // AI异常
        }

        // AI失败 → 人工
        return GradingResult.manual();
    }
}
