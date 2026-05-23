package org.example.examsystem.config;

import org.example.examsystem.service.IService.FaceDetectService;
import org.example.examsystem.utils.MailService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

/**
 * 测试配置类
 * 用于排除或Mock需要特殊依赖的服务（如OpenCV、邮件服务）
 */
@Configuration
public class TestConfig {

    @MockBean
    private FaceDetectService faceDetectService;
    
    @MockBean
    private MailService mailService;
}