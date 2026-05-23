package org.example.examsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.examsystem.config.TestConfig;
import org.example.examsystem.entity.AnswerRecord;
import org.example.examsystem.entity.Exam;
import org.example.examsystem.entity.ExamQuestion;
import org.example.examsystem.entity.Question;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.AnswerRecordMapper;
import org.example.examsystem.mapper.ExamMapper;
import org.example.examsystem.mapper.ExamQuestionMapper;
import org.example.examsystem.mapper.QuestionMapper;
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
public class JudgeExamIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TesterExamMapper testerExamMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User testTeacher;
    private User testStudent;
    private User testOtherTeacher;
    private Exam testExam;
    private TesterExam testerExam;
    private String teacherToken;
    private String studentToken;
    private String otherTeacherToken;
    private Long question1Id;
    private Long question2Id;

    @BeforeEach
    void setUp() {
        examMapper.delete(null);
        userMapper.delete(null);
        testerExamMapper.delete(null);
        questionMapper.delete(null);
        examQuestionMapper.delete(null);
        answerRecordMapper.delete(null);

        testTeacher = new User();
        testTeacher.setRealName("creator");
        testTeacher.setPassword("password123");
        testTeacher.setEmail("creator@example.com");
        testTeacher.setRole(2);
        userMapper.insert(testTeacher);

        testStudent = new User();
        testStudent.setRealName("student");
        testStudent.setPassword("password123");
        testStudent.setEmail("student@example.com");
        testStudent.setRole(3);
        userMapper.insert(testStudent);

        testOtherTeacher = new User();
        testOtherTeacher.setRealName("other_teacher");
        testOtherTeacher.setPassword("password123");
        testOtherTeacher.setEmail("other@example.com");
        testOtherTeacher.setRole(2);
        userMapper.insert(testOtherTeacher);

        testExam = new Exam();
        testExam.setExamName("测试考试");
        testExam.setExamCode(123456);
        testExam.setCreatorId(testTeacher.getId());
        testExam.setLimitMinutes(120);
        testExam.setStartTime(LocalDateTime.now().minusMinutes(30));
        examMapper.insert(testExam);

        testerExam = new TesterExam();
        testerExam.setExamId(testExam.getId());
        testerExam.setStudentId(testStudent.getId());
        testerExam.setStartTime(LocalDateTime.now().minusMinutes(60));
        testerExam.setStatus(1);
        testerExamMapper.insert(testerExam);

        Question q1 = new Question();
        q1.setContent("题目1内容");
        q1.setQuestionType(5);
        q1.setIsDeleted(0);
        questionMapper.insert(q1);
        question1Id = q1.getId();

        Question q2 = new Question();
        q2.setContent("题目2内容");
        q2.setQuestionType(5);
        q2.setIsDeleted(0);
        questionMapper.insert(q2);
        question2Id = q2.getId();

        ExamQuestion eq1 = new ExamQuestion();
        eq1.setExamId(testExam.getId());
        eq1.setQuestionId(q1.getId());
        eq1.setSort(1);
        eq1.setIsDeleted(0);
        examQuestionMapper.insert(eq1);

        ExamQuestion eq2 = new ExamQuestion();
        eq2.setExamId(testExam.getId());
        eq2.setQuestionId(q2.getId());
        eq2.setSort(2);
        eq2.setIsDeleted(0);
        examQuestionMapper.insert(eq2);

        AnswerRecord ar1 = new AnswerRecord();
        ar1.setStudentExamId(testerExam.getId());
        ar1.setQuestionId(q1.getId());
        ar1.setStudentAnswer("学生答案1");
        answerRecordMapper.insert(ar1);

        AnswerRecord ar2 = new AnswerRecord();
        ar2.setStudentExamId(testerExam.getId());
        ar2.setQuestionId(q2.getId());
        ar2.setStudentAnswer("学生答案2");
        answerRecordMapper.insert(ar2);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testTeacher.getId());
        teacherToken = JwtUtils.generateJwt(claims);

        claims.put("userId", testStudent.getId());
        studentToken = JwtUtils.generateJwt(claims);

        claims.put("userId", testOtherTeacher.getId());
        otherTeacherToken = JwtUtils.generateJwt(claims);
    }

    private String getTeacherAuthorizationHeader() {
        return "Bearer " + teacherToken;
    }

    private String getStudentAuthorizationHeader() {
        return "Bearer " + studentToken;
    }

    private String getOtherTeacherAuthorizationHeader() {
        return "Bearer " + otherTeacherToken;
    }

    private Map<String, Object> createJudgeRequest(Long testerId, List<Map<String, Object>> questions) {
        Map<String, Object> request = new HashMap<>();
        request.put("testerId", testerId);
        request.put("questions", questions);
        return request;
    }

    @Nested
    @DisplayName("JUDGE-001~003 正常场景")
    class NormalScenarioTests {

        @Test
        @DisplayName("JUDGE-001 - 单题评卷")
        void testJudgeExam_SingleQuestion() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            question.put("comment", "正确");
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("JUDGE-002 - 多题评卷")
        void testJudgeExam_MultipleQuestions() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();

            Map<String, Object> question1 = new HashMap<>();
            question1.put("questionId", question1Id);
            question1.put("userScore", 10.0);
            questions.add(question1);

            Map<String, Object> question2 = new HashMap<>();
            question2.put("questionId", question2Id);
            question2.put("userScore", 8.0);
            questions.add(question2);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("JUDGE-003 - 零分评卷")
        void testJudgeExam_ZeroScore() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 0.0);
            question.put("comment", "错误");
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("JUDGE-101~108 异常场景")
    class ExceptionScenarioTests {

        @Test
        @DisplayName("JUDGE-101 - 考试不存在")
        void testJudgeExam_ExamNotFound() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/999/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("JUDGE-102 - 考生不存在")
        void testJudgeExam_TesterNotFound() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(999L, questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("JUDGE-103 - questions为空")
        void testJudgeExam_EmptyQuestions() throws Exception {
            Map<String, Object> request = createJudgeRequest(testStudent.getId(), new ArrayList<>());

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("JUDGE-104 - 题目不存在")
        void testJudgeExam_QuestionNotFound() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", 999L);
            question.put("userScore", 10.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("JUDGE-105 - 负分")
        void testJudgeExam_NegativeScore() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", -1.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("JUDGE-107 - 无Token")
        void testJudgeExam_NoToken() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("JUDGE-108 - 非发卷人不能评卷")
        void testJudgeExam_NonCreatorJudge() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getOtherTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Nested
    @DisplayName("JUDGE-201~202 安全性场景")
    class SecurityScenarioTests {

        @Test
        @DisplayName("JUDGE-201 - SQL注入")
        void testJudgeExam_SQLInjection() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            question.put("comment", "' OR '1'='1");
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("JUDGE-202 - XSS攻击")
        void testJudgeExam_XSSAttack() throws Exception {
            List<Map<String, Object>> questions = new ArrayList<>();
            Map<String, Object> question = new HashMap<>();
            question.put("questionId", question1Id);
            question.put("userScore", 10.0);
            question.put("comment", "<script>alert(1)</script>");
            questions.add(question);

            Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

            mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                            .header("Authorization", getTeacherAuthorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("JUDGE-301~302 性能场景")
    class PerformanceScenarioTests {

        @Test
        @DisplayName("JUDGE-301 - 并发评卷")
        void testJudgeExam_ConcurrentJudge() throws Exception {
            int threadCount = 3;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        List<Map<String, Object>> questions = new ArrayList<>();
                        Map<String, Object> question = new HashMap<>();
                        question.put("questionId", question1Id);
                        question.put("userScore", 10.0);
                        questions.add(question);

                        Map<String, Object> request = createJudgeRequest(testStudent.getId(), questions);

                        mockMvc.perform(post("/exam/paper/" + testExam.getId() + "/judge")
                                        .header("Authorization", getTeacherAuthorizationHeader())
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
