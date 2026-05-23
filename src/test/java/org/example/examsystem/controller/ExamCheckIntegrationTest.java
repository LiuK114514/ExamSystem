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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class ExamCheckIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Exam testExam;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        examMapper.delete(null);
        userMapper.delete(null);

        testUser = new User();
        testUser.setRealName("testuser");
        testUser.setPassword("password123");
        testUser.setEmail("test@example.com");
        testUser.setRole(1);
        userMapper.insert(testUser);

        testExam = new Exam();
        testExam.setExamName("测试考试");
        testExam.setExamCode(123456);
        testExam.setCreatorId(testUser.getId());
        testExam.setLimitMinutes(120);
        testExam.setStartTime(java.time.LocalDateTime.now().plusMinutes(30));
        examMapper.insert(testExam);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUser.getId());
        jwtToken = JwtUtils.generateJwt(claims);
    }

    private String getAuthorizationHeader() {
        return "Bearer " + jwtToken;
    }

    @Nested
    @DisplayName("CHECK-001~002 正常场景")
    class NormalScenarioTests {

        @Test
        @DisplayName("CHECK-001 - 正常查询考试")
        void testCheckExam_NormalSuccess() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "123456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(notNullValue()));
        }

        @Test
        @DisplayName("CHECK-002 - 查询其他考试")
        void testCheckExam_OtherExam() throws Exception {
            Exam exam2 = new Exam();
            exam2.setExamName("另一个考试");
            exam2.setExamCode(654321);
            exam2.setCreatorId(testUser.getId());
            exam2.setLimitMinutes(90);
            exam2.setStartTime(java.time.LocalDateTime.now().plusHours(1));
            examMapper.insert(exam2);

            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "654321"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.examName").value("另一个考试"));
        }
    }

    @Nested
    @DisplayName("CHECK-101~107 异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("CHECK-101 - 考试码为空")
        void testCheckExam_EmptyCode() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("CHECK-102 - 考试码5位")
        void testCheckExam_Code5Digits() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "12345"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("CHECK-103 - 考试码7位")
        void testCheckExam_Code7Digits() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "1234567"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("CHECK-104 - 考试码非数字")
        void testCheckExam_CodeNonNumeric() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "abcdef"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("CHECK-105 - 考试不存在")
        void testCheckExam_NotFound() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("CHECK-106 - 无Token")
        void testCheckExam_NoToken() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .param("code", "123456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("CHECK-107 - 无效Token")
        void testCheckExam_InvalidToken() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", "Bearer invalid.token.here")
                            .param("code", "123456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    @Nested
    @DisplayName("CHECK-201 安全性场景")
    class SecurityScenarioTests {

        @Test
        @DisplayName("CHECK-201 - SQL注入")
        void testCheckExam_SQLInjection() throws Exception {
            mockMvc.perform(get("/exam/check")
                            .header("Authorization", getAuthorizationHeader())
                            .param("code", "' OR '1'='1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("CHECK-301 性能场景")
    class PerformanceScenarioTests {

        @Test
        @DisplayName("CHECK-301 - 并发查询")
        void testCheckExam_ConcurrentQuery() throws Exception {
            int threadCount = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        mockMvc.perform(get("/exam/check")
                                        .header("Authorization", getAuthorizationHeader())
                                        .param("code", "123456"))
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
