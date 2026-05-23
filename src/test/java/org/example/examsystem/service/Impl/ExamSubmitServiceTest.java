package org.example.examsystem.service.Impl;

import org.example.examsystem.dto.AnswerDTO;
import org.example.examsystem.dto.ExamSubmitMessage;
import org.example.examsystem.dto.SparkGradingResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.example.examsystem.service.Impl.ExamPaperServiceImpl;
import java.util.List;


class ExamSubmitServiceTest {

    private final SparkAiGradingService sparkAiGradingService = new SparkAiGradingService();

    @Test
    void testGradeSubjectiveAnswer() {
        SparkGradingResult result = sparkAiGradingService.gradeSubjectiveAnswer(
                "简述Spring IOC的概念及其优点",
                "IOC即控制反转，将对象的创建和依赖关系的管理交给Spring容器，降低耦合度，便于测试和维护",
                "IOC是控制反转，就是把对象交给Spring管理，不用自己new对象",
                10.0
        );

        System.out.println("是否成功: " + result.isSuccess());
        System.out.println("AI评分: " + result.getScore());
        System.out.println("评分理由: " + result.getReason());
    }
}