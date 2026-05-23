package org.example.examsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.examsystem.config.TestConfig;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class CreateExamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        examMapper.delete(null);
        userMapper.delete(null);

        testUser = new User();
        testUser.setRealName("testteacher");
        testUser.setPassword("password123");
        testUser.setEmail("teacher@example.com");
        testUser.setRole(2);
        userMapper.insert(testUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId());
        jwtToken = JwtUtils.generateJwt(claims);
    }

    private String getAuthorizationHeader() {
        return "Bearer " + jwtToken;
    }

    private Map<String, Object> createExamRequest(String examCode, String examName, String startDate, String startTime, int duration) {
        Map<String, Object> request = new HashMap<>();
        request.put("examCode", examCode);
        request.put("examName", examName);
        request.put("startDate", startDate);
        request.put("startTime", startTime);
        request.put("duration", duration);
        request.put("showAnswers", false);
        return request;
    }

    @Nested
    @DisplayName("EXAM-001~004 正常场景")
    class NormalScenarioTests {

        @Test
        @DisplayName("EXAM-001 - 正常创建考试")
        void testCreateExam_NormalSuccess() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学期末考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("EXAM-002 - 不同时段考试")
        void testCreateExam_DifferentTime() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "654321", "英语期中考试", "2026-06-15", "14:00", 90);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("EXAM-003 - 短时考试")
        void testCreateExam_ShortDuration() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "111111", "物理测验", "2026-06-20", "08:00", 45);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("EXAM-004 - 长时考试")
        void testCreateExam_LongDuration() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "222222", "化学实验", "2026-07-01", "10:00", 180);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("EXAM-101~109 异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("EXAM-101 - 考试码为空")
        void testCreateExam_EmptyCode() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-102 - 考试码5位")
        void testCreateExam_Code5Digits() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "12345", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-103 - 考试码7位")
        void testCreateExam_Code7Digits() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "1234567", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-104 - 考试码非数字")
        void testCreateExam_CodeNonNumeric() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "abcdef", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-105 - 考试名称为空")
        void testCreateExam_EmptyName() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-106 - 时长为0")
        void testCreateExam_ZeroDuration() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学考试", "2026-06-01", "09:00", 0);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-107 - 超长时长")
        void testCreateExam_TooLongDuration() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学考试", "2026-06-01", "09:00", 601);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-108 - 过去日期")
        void testCreateExam_PastDate() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学考试", "2020-01-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("EXAM-109 - 重复考试码")
        void testCreateExam_DuplicateCode() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            request.put("examName", "另一个考试");
            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(409));
        }
    }

    @Nested
    @DisplayName("EXAM-201~203 安全性场景")
    class SecurityScenarioTests {

        @Test
        @DisplayName("EXAM-201 - SQL注入")
        void testCreateExam_SQLInjection() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "' OR '1'='1", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EXAM-202 - XSS攻击")
        void testCreateExam_XSSAttack() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "<script>alert(1)</script>", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("EXAM-203 - 无Token")
        void testCreateExam_NoToken() throws Exception {
            Map<String, Object> request = createExamRequest(
                    "123456", "数学考试", "2026-06-01", "09:00", 120);

            mockMvc.perform(post("/test/exams")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    @Nested
    @DisplayName("EXAM-301~302 性能场景")
    class PerformanceScenarioTests {

        @Test
        @DisplayName("EXAM-301 - 并发创建考试")
        void testCreateExam_ConcurrentCreate() throws Exception {
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        String code = String.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000));
                        Map<String, Object> request = createExamRequest(
                                code, "并发考试" + index, "2026-06-01", "09:00", 120);

                        mockMvc.perform(post("/test/exams")
                                        .header("Authorization", getAuthorizationHeader())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }
        }
    }
}
