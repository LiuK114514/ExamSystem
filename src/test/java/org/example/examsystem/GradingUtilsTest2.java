package org.example.examsystem;

import org.example.examsystem.dto.GradingResult;
import org.example.examsystem.dto.QuestionAnswerDTO;
import org.example.examsystem.dto.SparkGradingResult;
import org.example.examsystem.service.Impl.SparkAiGradingService;
import org.example.examsystem.utils.GradingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GradingUtilsTest2 {
    @Mock
    private SparkAiGradingService aiService;

    // -----------------工具方法 -----------------
    private QuestionAnswerDTO build(int type, String correct, double score) {
        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(type);
        q.setCorrectAnswer(correct);
        q.setScore(score);
        q.setContent("test content");
        return q;
    }

    // ----------------- P10 空答案 -----------------
    @Test
    void TC_EMPTY() {
        QuestionAnswerDTO q = build(1, "A", 10);

        GradingResult result = GradingUtils.grade(q, "   ", aiService);

        assertEquals(0, result.getScore());
    }

    // ----------------- P1 单选、判断正确 -----------------
    @Test
    void TC_OBJ_CORRECT() {
        QuestionAnswerDTO q = build(1, "A", 10);

        GradingResult result = GradingUtils.grade(q, "A", aiService);

        assertEquals(10, result.getScore());
    }

    // ----------------- P2 单选、判断错误 -----------------
    @Test
    void TC_OBJ_WRONG() {
        QuestionAnswerDTO q = build(1, "A", 10);

        GradingResult result = GradingUtils.grade(q, "B", aiService);

        assertEquals(0, result.getScore());
    }

    // ----------------- P3 多选全对 -----------------
    @Test
    void TC_MC_FULL() {
        QuestionAnswerDTO q = build(2, "A,B,C", 10);

        GradingResult result = GradingUtils.grade(q, "A,B,C", aiService);

        assertEquals(10, result.getScore());
    }

    // ----------------- P4 多选全错 -----------------
    @Test
    void TC_MC_INVALID() {
        QuestionAnswerDTO q = build(2, "A,B,C", 10);

        GradingResult result = GradingUtils.grade(q, "A,B,D", aiService);

        assertEquals(0, result.getScore());
    }

    // ----------------- P5 多选对一半 -----------------
    @Test
    void TC_MC_HALF() {
        QuestionAnswerDTO q = build(2, "A,B,C", 10);

        GradingResult result = GradingUtils.grade(q, "A,B", aiService);

        assertEquals(5, result.getScore());
    }

    // ----------------- P6 填空ai批改成功 -----------------
    @Test
    void TC_FB_AI_OK() {
        QuestionAnswerDTO q = build(4, "answer", 10);

        SparkGradingResult ai = new SparkGradingResult(true,8.0,"ok");

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenReturn(ai);

        GradingResult result = GradingUtils.grade(q, "student", aiService);

        assertEquals(8, result.getScore());
    }

    // ----------------- P7 填空ai批改失败 -----------------
    @Test
    void TC_FB_AI_FAIL() {
        QuestionAnswerDTO q = build(4, "answer", 10);

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenThrow(new RuntimeException("AI error"));

        GradingResult result = GradingUtils.grade(q, "student", aiService);

        assertTrue(result.isManual());
    }

    // -------------- P8 简答ai批改成功 --------------
    @Test
    void TC_SUB_AI_OK() {
        QuestionAnswerDTO q = build(5, "answer", 10);

        SparkGradingResult ai = new SparkGradingResult(true, 9.0, "good");

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenReturn(ai);

        GradingResult result = GradingUtils.grade(q, "student", aiService);

        assertEquals(9, result.getScore());
    }

    // -------------- P9 简答ai批改失败 --------------
    @Test
    void TC_SUB_AI_FAIL() {
        QuestionAnswerDTO q = build(5, "answer", 10);

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenThrow(new RuntimeException("fail"));

        GradingResult result = GradingUtils.grade(q, "student", aiService);

        assertTrue(result.isManual());
    }

    // -------------- P11 默认分支 --------------
    @Test
    void TC_DEFAULT() {
        QuestionAnswerDTO q = build(99, "A", 10);

        GradingResult result = GradingUtils.grade(q, "A", aiService);

        assertEquals(0, result.getScore());
    }
}

