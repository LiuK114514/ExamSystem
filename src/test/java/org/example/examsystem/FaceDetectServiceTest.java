package org.example.examsystem;

import org.example.examsystem.service.IService.FaceDetectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class FaceDetectServiceTest {
    //将图像转为base64格式
    private String toBase64(String fileName) {
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException("文件不存在: " + fileName);
            }
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Autowired
    private FaceDetectService faceDetectService;

    @ParameterizedTest
    @ValueSource(strings = {
            "images/face_1.png",
            "images/face_1.2.png",
            "images/face_1.3.png",
            "images/face_1.4.png"
    })
    @DisplayName("TC-801 单人脸等价类（光照/遮挡）")
    void testSingleFaceVariants(String fileName) {
        String base64 = toBase64(fileName);
        assertEquals(1, faceDetectService.detectFace(base64));
    }
    @Test
    @DisplayName("TC-802 无人脸")
    void testNoFace() {
        String base64 = toBase64("images/face_0.png");
        assertEquals(0, faceDetectService.detectFace(base64));
    }
    @Test
    @DisplayName("TC-803 多人脸")
    void testMultiFace() {
        String base64 = toBase64("images/face_2.png");
        assertTrue(faceDetectService.detectFace(base64) >= 2);
    }
    @Test
    @DisplayName("TC-804 非法base64")
    void testInvalidBase64() {
        assertEquals(0, faceDetectService.detectFace("abc"));
    }

    @Test
    @DisplayName("TC-805 图像损坏")
    void testCorruptedImage() {
        String base64 = "data:image/jpeg;base64,xxxx";
        assertEquals(0, faceDetectService.detectFace(base64));
    }

    @Test
    @DisplayName("TC-806 空输入")
    void testNullInput() {
        assertEquals(0, faceDetectService.detectFace(null));
    }

}
