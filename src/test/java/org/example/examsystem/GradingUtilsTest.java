package org.example.examsystem;

import org.example.examsystem.dto.GradingResult;
import org.example.examsystem.dto.QuestionAnswerDTO;
import org.example.examsystem.dto.SparkGradingResult;
import org.example.examsystem.service.Impl.SparkAiGradingService;
import org.example.examsystem.utils.GradingUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GradingUtilsTest {
    // ===================== 测试用例结构 =====================
    private SparkAiGradingService mockAi() {
        return mock(SparkAiGradingService.class);
    }
    //------单选：正确--------
    @Test
    void TC501_singleChoice_correct() {

        SparkAiGradingService aiService = mockAi();

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(1);
        q.setCorrectAnswer("A");
        q.setScore(2.0);

        double result = GradingUtils.grade(q, "A", aiService).getScore();

        assertEquals(2.0, result, 0.0001);
    }
    //----------单选：错误-----------
    @Test
    void TC502_singleChoice_wrong() {

        SparkAiGradingService aiService = mockAi();

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(1);
        q.setCorrectAnswer("A");
        q.setScore(2.0);

        double result = GradingUtils.grade(q, "B", aiService).getScore();

        assertEquals(0.0, result, 0.0001);
    }
    //---------TC-503 多选全对-----------
    @Test
    void TC503_multiChoice_allCorrect() {

        SparkAiGradingService aiService = mockAi();

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(2);
        q.setCorrectAnswer("A,B,C");
        q.setScore(10.0);

        double result = GradingUtils.grade(q, "A,B,C", aiService).getScore();

        assertEquals(10.0, result, 0.0001);
    }
    //---------TC-504 多选漏选-----------
    @Test
    void TC504_multiChoice_partial() {

        SparkAiGradingService aiService = mockAi();

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(2);
        q.setCorrectAnswer("A,B,C");
        q.setScore(10.0);

        double result = GradingUtils.grade(q, "A,B", aiService).getScore();

        assertEquals(5.0, result, 0.0001);
    }
    //-------TC-505 多选错选-----------
    @Test
    void TC505_multiChoice_wrongOption() {

        SparkAiGradingService aiService = mockAi();

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(2);
        q.setCorrectAnswer("A,B,C");
        q.setScore(10.0);

        double result = GradingUtils.grade(q, "A,B,D", aiService).getScore();

        assertEquals(0.0, result, 0.0001);
    }
    //--------TC-506 填空题---------
    @Test
    void test_fill_blank() {

        SparkAiGradingService aiService = mock(SparkAiGradingService.class);

        SparkGradingResult ai = new SparkGradingResult(true, 5.0, "ok");

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenReturn(ai);

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(4);
        q.setCorrectAnswer("北京");
        q.setContent("中国首都");
        q.setScore(5.0);
        double score = GradingUtils.grade(q, " 北京 ", aiService).getScore();
        assertEquals(5.0, score, 0.0001);
    }
    //----------TC-510 AI失败---------
    @Test
    void test_ai_failure_fallback_manual() {

        SparkAiGradingService aiService = mock(SparkAiGradingService.class);

        when(aiService.gradeSubjectiveAnswer(any(), any(), any(), anyDouble()))
                .thenThrow(new RuntimeException("AI error"));

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(5);
        q.setCorrectAnswer("标准");
        q.setContent("题目");
        q.setScore(10.0);

        GradingResult result = GradingUtils.grade(q, "答案", aiService);

        assertTrue(result.isManual());
    }
    //--------TC-512 空答案----------
    @Test
    void test_blank_answer() {

        SparkAiGradingService aiService = mock(SparkAiGradingService.class);

        QuestionAnswerDTO q = new QuestionAnswerDTO();
        q.setQuestionType(1);
        q.setCorrectAnswer("A");
        q.setScore(2.0);

        GradingResult result = GradingUtils.grade(q, "", aiService);

        assertEquals(0.0, result.getScore(), 0.0001);
    }
}
