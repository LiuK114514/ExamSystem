package org.example.examsystem.service.IService;

public interface FaceDetectService {
    /**
     * 检测图片中人脸数量
     * @param base64 图片base64
     * @return 人脸数量
     */
    int detectFace(String base64);
}
