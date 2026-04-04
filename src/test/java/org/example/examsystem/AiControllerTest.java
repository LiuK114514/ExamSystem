package org.example.examsystem;

import org.example.examsystem.controller.AiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGetAiAnswer() {
        AiController controller = new AiController();

        // 模拟前端传过来的数据
        Map<String, Object> question = new HashMap<>();
        question.put("type", "single");
        question.put("content", "Java中用于创建线程的类是？");
        question.put("answer", "A");

        List<String> userAnswers = new ArrayList<>();
        userAnswers.add("A"); // 假设考生答对了

        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        body.put("answerStore.userAnswers", userAnswers);
        body.put("index", 0);
        body.put("userId", "testUser");
        body.put("stream", false);

        // 调用接口
        ResponseEntity<?> response = controller.getAiAnswer(body);

        // 输出返回内容
        System.out.println(response.getBody());
    }
}