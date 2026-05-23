package org.example.examsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.examsystem.config.TestConfig;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.TesterExamMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ExamSubmitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TesterExamMapper testerExamMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User testCreator;
    private Exam testExam;
    private TesterExam testerExam;
    private String jwtToken;
    private String creatorToken;

    @BeforeEach
    void setUp() {
        examMapper.delete(null);
        userMapper.delete(null);
        testerExamMapper.delete(null);

        testCreator = new User();
        testCreator.setRealName("teacher");
        testCreator.setPassword("password123");
        testCreator.setEmail("teacher@example.com");
        testCreator.setRole(2);
        userMapper.insert(testCreator);

        testUser = new User();
        testUser.setRealName("student");
        testUser.setPassword("password123");
        testUser.setEmail("student@example.com");
        testUser.setRole(3);
        userMapper.insert(testUser);

        testExam = new Exam();
        testExam.setExamName("测试考试");
        testExam.setExamCode(123456);
        testExam.setCreatorId(testCreator.getId());
        testExam.setLimitMinutes(120);
        testExam.setStartTime(LocalDateTime.now().minusMinutes(30));
        examMapper.insert(testExam);

        testerExam = new TesterExam();
        testerExam.setExamId(testExam.getId());
        testerExam.setStudentId(testUser.getId());
        testerExam.setStartTime(LocalDateTime.now().minusMinutes(60));
        testerExam.setStatus(1);
        testerExamMapper.insert(testerExam);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId());
        jwtToken = JwtUtils.generateJwt(claims);

        claims.put("userId", testCreator.getId());
        creatorToken = JwtUtils.generateJwt(claims);
    }

    private String getAuthorizationHeader() {
        return "Bearer " + jwtToken;
    }

    private Map<String, Object> createSubmitRequest(Long examId, List<Map<String, Object>> answers) {
        Map<String, Object> request = new HashMap<>();
        request.put("examId", examId);
        request.put("answers", answers);
        request.put("submitTime", System.currentTimeMillis());
        request.put("duration", 60);
        return request;
    }

    @Nested
    @DisplayName("SUBMIT-001~003 正常场景")
    class NormalScenarioTests {

        @Test
        @DisplayName("SUBMIT-001 - 正常提交")
        void testSubmitPaper_NormalSuccess() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", 1);
            answer.put("answer", "A");
            answers.add(answer);

            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("SUBMIT-002 - 多题提交")
        void testSubmitPaper_MultipleAnswers() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();

            Map<String, Object> answer1 = new HashMap<>();
            answer1.put("questionId", 1);
            answer1.put("answer", "B");
            answers.add(answer1);

            Map<String, Object> answer2 = new HashMap<>();
            answer2.put("questionId", 2);
            answer2.put("answer", "C");
            answers.add(answer2);

            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("SUBMIT-003 - 空答案提交")
        void testSubmitPaper_EmptyAnswers() throws Exception {
            Map<String, Object> request = createSubmitRequest(testExam.getId(), new ArrayList<>());

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("SUBMIT-101~105 异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("SUBMIT-101 - examId为空")
        void testSubmitPaper_EmptyExamId() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", 1);
            answer.put("answer", "A");
            answers.add(answer);

            Map<String, Object> request = new HashMap<>();
            request.put("examId", "");
            request.put("answers", answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("SUBMIT-102 - 考试不存在")
        void testSubmitPaper_ExamNotFound() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> request = createSubmitRequest(999L, answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("SUBMIT-103 - 题目不存在")
        void testSubmitPaper_QuestionNotFound() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", 999);
            answer.put("answer", "A");
            answers.add(answer);

            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("SUBMIT-104 - 无Token")
        void testSubmitPaper_NoToken() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("SUBMIT-201~202 安全性场景")
    class SecurityScenarioTests {

        @Test
        @DisplayName("SUBMIT-201 - SQL注入")
        void testSubmitPaper_SQLInjection() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", 1);
            answer.put("answer", "' OR '1'='1");
            answers.add(answer);

            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SUBMIT-202 - XSS攻击")
        void testSubmitPaper_XSSAttack() throws Exception {
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", 1);
            answer.put("answer", "<script>alert(1)</script>");
            answers.add(answer);

            Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

            mockMvc.perform(post("/exam/submit/paper")
                            .header("Authorization", getAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("SUBMIT-301~302 性能场景")
    class PerformanceScenarioTests {

        @Test
        @DisplayName("SUBMIT-301 - 并发提交")
        void testSubmitPaper_ConcurrentSubmit() throws Exception {
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        List<Map<String, Object>> answers = new ArrayList<>();
                        Map<String, Object> answer = new HashMap<>();
                        answer.put("questionId", 1);
                        answer.put("answer", "A");
                        answers.add(answer);

                        Map<String, Object> request = createSubmitRequest(testExam.getId(), answers);

                        mockMvc.perform(post("/exam/submit/paper")
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
