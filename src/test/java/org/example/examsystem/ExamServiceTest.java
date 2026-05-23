package org.example.examsystem;

import org.example.examsystem.dto.CreateExamRequest;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.mapper.*;
import org.example.examsystem.service.Impl.ExamServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExamServiceTest {
    private CreateExamRequest validRequest() {

        CreateExamRequest request = new CreateExamRequest();

        request.setExamName("期末考试");
        request.setExamCode("123456");
        request.setCreatorId(1L);
        request.setDuration(120);
        request.setStartDate("2099-01-01");
        request.setStartTime("10:00:00");
        request.setDescription("正常考试");
        request.setShowAnswers(true);
        request.setQuestions(new ArrayList<>());

        return request;
    }
    @InjectMocks
    private ExamServiceImpl examService;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private QuestionOptionMapper questionOptionMapper;

    @Mock
    private QuestionAnswerMapper questionAnswerMapper;

    @Mock
    private ExamQuestionMapper examQuestionMapper;

    //TC-201 正常创建
    @Test
    void TC_201_success_create_exam() {

        CreateExamRequest request = validRequest();
        request.setExamCode("123456");
        when(examMapper.selectCount(any())).thenReturn(0L);
        when(examMapper.insert(any(Exam.class))).thenAnswer(inv -> {
            Exam e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        });
        when(questionMapper.getQuestions(anyLong())).thenReturn(new ArrayList<>());

        Map<String, Object> result = examService.createExam(request);

        assertNotNull(result);
        assertEquals(123456, result.get("examCode"));
    }
    //TC-202 名称为空
    @Test
    void TC_202_examName_empty() {

        CreateExamRequest request = validRequest();
        request.setExamName("");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examService.createExam(request)
        );

        assertEquals("考试名称不能为空", ex.getMessage());
    }
    //TC-203 名称超长
    @Test
    void TC_203_examName_too_long() {

        CreateExamRequest request = validRequest();
        request.setExamName("A".repeat(51));
        assertThrows(IllegalArgumentException.class,
                () -> examService.createExam(request));
    }
    //TC-204 时长=0
    @Test
    void TC_204_duration_zero() {

        CreateExamRequest request = validRequest();
        request.setDuration(0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examService.createExam(request)
        );

        assertEquals("考试时长必须大于0", ex.getMessage());
    }
    //TC-205 时长=601
    @Test
    void TC_205_duration_too_large() {

        CreateExamRequest request = validRequest();
        request.setDuration(601);

        assertThrows(IllegalArgumentException.class,
                () -> examService.createExam(request));
    }
    //TC-206 结束早于开始
    @Test
    void TC_206_invalid_time() {

        CreateExamRequest request = validRequest();
        request.setStartDate("2099-01-01");
        request.setStartTime("99:99:99"); // 故意错误时间

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examService.createExam(request)
        );

        assertEquals("开始时间格式错误，应为 yyyy-MM-dd 与 HH:mm:ss", ex.getMessage());
    }
    //TC-207 题目列表为空
    @Test
    void TC_207_empty_questions() {

        CreateExamRequest request = validRequest();
        request.setQuestions(Collections.emptyList());

        when(examMapper.selectCount(any())).thenReturn(0L);

        when(examMapper.insert(any(Exam.class))).thenAnswer(inv -> {
            ((Exam)inv.getArgument(0)).setId(1L);
            return 1;
        });
        Map<String, Object> result = examService.createExam(request);

        assertNotNull(result);
        assertEquals(123456, result.get("examCode"));
    }
    //TC-208 考试码唯一性
    @Test
    void TC_208_examCode_duplicate() {

        CreateExamRequest request = validRequest();
        request.setExamCode("123456");

        when(examMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examService.createExam(request)
        );

        assertEquals("考试码已存在", ex.getMessage());
    }
}
