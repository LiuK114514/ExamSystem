package org.example.examsystem.service.Impl;

import cn.hutool.http.body.RequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.examsystem.dto.SparkGradingResult;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SparkAiGradingService {

    private static final String API_URL = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    private static final String API_PASSWORD = "UHxnxXMcugqTZpXtZwnz:VBgaDukATLINpTZpGiLr";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SparkGradingResult gradeSubjectiveAnswer(
            String questionContent,
            String referenceAnswer,
            String studentAnswer,
            Double fullScore) {

        String prompt = buildGradingPrompt(questionContent, referenceAnswer, studentAnswer, fullScore);

        try {
            String aiResponse = callSparkApi(prompt);
            return parseGradingResult(aiResponse, fullScore);
        } catch (Exception e) {
            log.error("讯飞星火评分失败, question={}, error={}", questionContent, e.getMessage(), e);
            return SparkGradingResult.failed();
        }
    }

    private String callSparkApi(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "spark-x");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_PASSWORD);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        return response.getBody();
    }

    private SparkGradingResult parseGradingResult(String aiResponse, Double fullScore) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            String content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            JsonNode result = objectMapper.readTree(content);
            double score = result.path("score").asDouble();
            String reason = result.path("reason").asText();

            score = Math.max(0, Math.min(score, fullScore));

            return SparkGradingResult.success(score, reason);
        } catch (Exception e) {
            log.warn("解析星火评分结果失败, response={}", aiResponse, e);
            return SparkGradingResult.failed();
        }
    }

    private String buildGradingPrompt(String questionContent, String referenceAnswer,
                                      String studentAnswer, Double fullScore) {
        return String.format("""
                你是一位专业的考试评分助手，请根据参考答案对学生的主观题答案进行评分。
                
                【评分规则】
                1. 满分为 %.1f 分，评分必须为0到满分之间的数值（允许小数，精确到0.5分）
                2. 根据答案的完整性、准确性、逻辑性综合评分
                3. 必须给出简短的评分理由（不超过100字）
                
                【题目内容】
                %s
                
                【参考答案】
                %s
                
                【学生答案】
                %s
                
                【输出格式】请严格按照以下JSON格式输出，不要包含其他内容：
                {"score": 评分数值, "reason": "评分理由"}
                """,
                fullScore, questionContent, referenceAnswer, studentAnswer);
    }
}