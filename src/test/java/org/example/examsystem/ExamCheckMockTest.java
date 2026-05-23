package org.example.examsystem;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.examsystem.controller.ExamController;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.TesterExamMapper;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.service.IService.IExamService;
import org.example.examsystem.vo.ExamSimpleInfoVO;
import org.example.examsystem.vo.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("考试码查询考试Mock测试")
class ExamCheckMockTest {

    private Exam mockExam(String code, LocalDateTime startTime) {
        Exam exam = new Exam();
        exam.setId(1L);
        exam.setExamCode(Integer.valueOf(code));
        exam.setStartTime(startTime);
        exam.setCreatorId(1L);
        exam.setStatus(0);
        return exam;
    }

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        return user;
    }

    @InjectMocks
    private ExamController examController;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TesterExamMapper testerExamMapper;

    private final Long mockUserId = 1001L;

    private LocalDateTime now;

    @BeforeEach
    @DisplayName("初始化测试环境时间")
    void setUp() {
        now = LocalDateTime.of(2024, 1, 1, 10, 0);
    }

    // TC-301: 正常查询考试
    @Test
    @DisplayName("TC-301: 正常查询考试（未开始状态）")
    void testQueryExamSuccess() {
        Exam exam = mockExam("123456", LocalDateTime.now().plusDays(1));
        when(examMapper.selectOne(any())).thenReturn(exam);
        when(userMapper.selectById(any())).thenReturn(mockUser());
        when(testerExamMapper.selectOne(any())).thenReturn(null);

        Result result = examController.checkExam(1L, "123456");

        ExamSimpleInfoVO vo = (ExamSimpleInfoVO) result.getData();

        assertEquals(0, vo.getStatus());
    }

    // TC-305: 考试码不存在
    @Test
    @DisplayName("TC-305: 考试码不存在")
    void testExamCodeNotFound() {
        String examCode = "999999";

        when(examMapper.selectOne(any())).thenReturn(null);

        Result result = examController.checkExam(mockUserId, examCode);

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo("未找到该考试！");

        verify(examMapper, times(1)).selectOne(any());
    }

    // TC-302: 考试码5位
    @Test
    @DisplayName("TC-302: 考试码长度为5位（格式错误）")
    void testExamCodeLength5() {
        String examCode = "12345";

        Result result = examController.checkExam(mockUserId, examCode);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("考试码必须为6位");

        verify(examMapper, never()).selectOne(any());
    }

    // TC-303: 考试码7位
    @Test
    @DisplayName("TC-303: 考试码长度为7位（格式错误）")
    void testExamCodeLength7() {
        String examCode = "1234567";

        Result result = examController.checkExam(mockUserId, examCode);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("考试码必须为6位");

        verify(examMapper, never()).selectOne(any());
    }

    //TC-304: 非数字考试码
    @Test
    @DisplayName("TC-304: 考试码含非数字字符（格式错误）")
    void testExamCodeInvalidFormat() {
        String examCode = "AB1234";

        Result result = examController.checkExam(mockUserId, examCode);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("考试码必须为6位数字");

        verify(examMapper, never()).selectOne(any());
    }

    // TC-307: 未作答考试
    @Test
    @DisplayName("TC-307: 考试进行中")
    void testExamNoTesterRecord() {

        Exam exam = mockExam("123456", now.minusDays(1));
        TesterExam te = new TesterExam();
        te.setStatus(1);
        when(examMapper.selectOne(any())).thenReturn(exam);
        when(userMapper.selectById(any())).thenReturn(mockUser());
        when(testerExamMapper.selectOne(any())).thenReturn(te);

        Result result = examController.checkExam(1L, "123456");

        ExamSimpleInfoVO vo = (ExamSimpleInfoVO) result.getData();

        assertEquals(1, vo.getStatus());
    }


    //TC-309: 已完成考试

    @Test
    @DisplayName("TC-309: 已完成考试")
    void testExamFinished() {

        Exam exam = mockExam("123456", now.minusDays(1));
        TesterExam te = new TesterExam();
        te.setStatus(2);
        when(examMapper.selectOne(any())).thenReturn(exam);
        when(userMapper.selectById(any())).thenReturn(mockUser());
        when(testerExamMapper.selectOne(any())).thenReturn(te);

        Result result = examController.checkExam(1L, "123456");

        ExamSimpleInfoVO vo = (ExamSimpleInfoVO) result.getData();

        assertEquals(2, vo.getStatus());
    }
}