package org.example.examsystem;

import org.example.examsystem.config.EmbeddedRedisConfig;
import org.example.examsystem.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestConfig.class, EmbeddedRedisConfig.class})
class ExamSystemApplicationTests {

    @Test
    void contextLoads() {
    }

}
