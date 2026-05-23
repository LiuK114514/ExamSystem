package org.example.examsystem.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的验证码存储与校验（内存版，5分钟有效）
 */
@Component
public class VerificationCodeService {

    private static final long EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final long RATE_LIMIT_WINDOW_MILLIS = 60 * 1000L; // 1分钟时间窗口
    private static final int MAX_REQUESTS_PER_WINDOW = 10; // 每个时间窗口内最多请求次数
    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> rateLimitStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 生成6位数字验证码并保存（带频率限制：1分钟内最多10次）
     */
    public String generate(String email) {
        long currentTime = System.currentTimeMillis();

        // 获取该邮箱的请求时间列表
        List<Long> requestTimes = rateLimitStore.computeIfAbsent(email, k -> new ArrayList<>());

        // 清理过期的请求记录（只保留1分钟内的）
        requestTimes.removeIf(time -> (currentTime - time) >= RATE_LIMIT_WINDOW_MILLIS);

        // 检查是否超过频率限制
        if (requestTimes.size() >= MAX_REQUESTS_PER_WINDOW) {
            throw new IllegalArgumentException("验证码请求过于频繁，请稍后再试");
        }

        // 记录本次请求时间
        requestTimes.add(currentTime);

        // 生成验证码
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(email, new CodeEntry(code, currentTime + EXPIRE_MILLIS));

        return code;
    }

    /**
     * 校验验证码是否匹配且未过期
     */
    public boolean validate(String email, String code) {
        CodeEntry entry = store.get(email);
        if (entry == null) {
            return false;
        }
        if (entry.expireAt < Instant.now().toEpochMilli()) {
            store.remove(email);
            return false;
        }
        boolean ok = entry.code.equals(code);
        if (ok) {
            store.remove(email); // 用后即焚
        }
        return ok;
    }

    private static class CodeEntry {
        String code;
        long expireAt;

        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}

