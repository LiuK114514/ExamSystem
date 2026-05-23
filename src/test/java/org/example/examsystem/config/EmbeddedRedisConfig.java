package org.example.examsystem.config;

import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * 嵌入式Redis配置类
 * 在测试环境中自动启动嵌入式Redis服务器
 */
@Configuration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        // 使用随机端口避免冲突
        redisServer = RedisServer.builder()
                .port(6379)
                .setting("maxmemory 128mb")
                .build();
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}