package org.example.examsystem.config;

import org.example.examsystem.utils.VerificationCodeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试专用的验证码服务
 * 固定返回验证码123456，便于测试
 * 实现限流逻辑以支持限流测试
 */
@Component
@Primary
public class TestVerificationCodeService extends VerificationCodeService {

    private static final String TEST_CODE = "123456";
    private static final long RATE_LIMIT_WINDOW_MILLIS = 60 * 1000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private final Map<String, List<Long>> rateLimitStore = new ConcurrentHashMap<>();

    @Override
    public String generate(String email) {
        long currentTime = System.currentTimeMillis();
        
        List<Long> requestTimes = rateLimitStore.computeIfAbsent(email, k -> new ArrayList<>());
        requestTimes.removeIf(time -> (currentTime - time) >= RATE_LIMIT_WINDOW_MILLIS);
        
        if (requestTimes.size() >= MAX_REQUESTS_PER_WINDOW) {
            throw new IllegalArgumentException("验证码请求过于频繁，请稍后再试");
        }
        
        requestTimes.add(currentTime);
        return TEST_CODE;
    }

    @Override
    public boolean validate(String email, String code) {
        return TEST_CODE.equals(code);
    }
}