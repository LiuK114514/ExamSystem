package org.example.examsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.examsystem.config.TestConfig;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.UserMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String testEmail = "test" + System.currentTimeMillis() + "@example.com";

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
    }

    @Nested
    @DisplayName("REG-001~004 正常场景")
    class NormalScenarioTests {

        @Test
        @DisplayName("REG-001 - 正常注册流程")
        void testRegister_NormalSuccess() throws Exception {
            String email = "test01@example.com";

            // 直接使用正确的验证码注册（测试环境中验证码固定为123456）
            Map<String, Object> request = new HashMap<>();
            request.put("email", email);
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("REG-002 - 不同用户注册")
        void testRegister_DifferentUser() throws Exception {
            String email = "test02@example.com";

            Map<String, Object> request = new HashMap<>();
            request.put("email", email);
            request.put("password", "Test@456");
            request.put("realname", "李四");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("REG-003 - 不同域名邮箱")
        void testRegister_DifferentDomain() throws Exception {
            String email = "user@domain.cn";

            Map<String, Object> request = new HashMap<>();
            request.put("email", email);
            request.put("password", "MyPass1!");
            request.put("realname", "王五");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("REG-101~108 异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("REG-101 - 邮箱为空")
        void testRegister_EmptyEmail() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-102 - 无效邮箱格式")
        void testRegister_InvalidEmail() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "invalid-email");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-103 - 密码为空")
        void testRegister_EmptyPassword() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-104 - 密码太短")
        void testRegister_PasswordTooShort() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "123");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-105 - 真实姓名为空")
        void testRegister_EmptyRealName() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "Test123!");
            request.put("realname", "");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("REG-106 - 验证码为空")
        void testRegister_EmptyCode() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-107 - 错误验证码")
        void testRegister_WrongCode() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "000000");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("REG-108 - 重复注册")
        void testRegister_DuplicateUser() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "existing@example.com");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            request.put("verificationCode", "123456");
            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(409));
        }
    }

    @Nested
    @DisplayName("REG-201~204 安全性场景")
    class SecurityScenarioTests {

        @Test
        @DisplayName("REG-201 - SQL注入-验证码")
        void testRegister_SQLInjection_Code() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "' OR '1'='1");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("REG-202 - SQL注入-邮箱")
        void testRegister_SQLInjection_Email() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "' OR '1'='1'--");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("REG-203 - XSS攻击-真实姓名")
        void testRegister_XSS_RealName() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "test@example.com");
            request.put("password", "Test123!");
            request.put("realname", "<script>alert(1)</script>");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("REG-204 - XSS攻击-邮箱")
        void testRegister_XSS_Email() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("email", "<script>@example.com");
            request.put("password", "Test123!");
            request.put("realname", "张三");
            request.put("verificationCode", "123456");

            mockMvc.perform(post("/employee/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("REG-301~302 性能场景")
    class PerformanceScenarioTests {

        @Test
        @DisplayName("REG-301 - 并发注册100用户")
        void testRegister_ConcurrentUsers() throws Exception {
            int threadCount = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    try {
                        Map<String, Object> request = new HashMap<>();
                        request.put("email", "concurrent" + index + "@example.com");
                        request.put("password", "Test123!");
                        request.put("realname", "用户" + index);
                        request.put("verificationCode", "123456");

                        mockMvc.perform(post("/employee/register")
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
        @Test
        @DisplayName("REG-302 - 请求限流测试（1分钟内超过10次请求）")
        void testRegister_RateLimit() throws Exception {
            String email = "ratelimit@example.com";
    
            // 前10次请求应该成功
            for (int i = 0; i < 10; i++) {
                Map<String, Object> request = new HashMap<>();
                request.put("email", email);
                
                mockMvc.perform(post("/employee/register/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(200));
            }
            
                // 第11次请求应该触发限流
                Map<String, Object> request = new HashMap<>();
                request.put("email", email);
                
                mockMvc.perform(post("/employee/register/code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(500))  // 限流返回500（IllegalArgumentException被全局异常处理器处理）
                        .andExpect(jsonPath("$.message").value("验证码请求过于频繁，请稍后再试"));
            }
    }
}
