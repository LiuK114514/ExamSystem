package org.example.examsystem.service.Impl;

import io.lettuce.core.output.ScoredValueScanOutput;
import org.example.examsystem.service.IService.FaceDetectService;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@Service
public class FaceDetectServiceImpl implements FaceDetectService {
    // 单例，避免重复加载模型
    private static final Net faceNet;
    static {
        // 加载 OpenCV 动态库
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        try {
            File protoFile = ResourceUtils.getFile(
                    "classpath:dnn/deploy.prototxt.txt");

            File modelFile = ResourceUtils.getFile(
                    "classpath:dnn/res10_300x300_ssd_iter_140000.caffemodel");

            faceNet = Dnn.readNetFromCaffe(
                    protoFile.getAbsolutePath(),
                    modelFile.getAbsolutePath()
            );

        } catch (Exception e) {
            throw new RuntimeException("DNN初始化失败", e);
        }
    }

    @Override
    public int detectFace(String base64) {
        try {
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }

            byte[] bytes = Base64.getDecoder().decode(base64);

            Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);

            if (mat.empty()) {
                return 0;
            }

            // 1. 预处理（DNN必须300x300）
            Mat blob = Dnn.blobFromImage(
                    mat,
                    1.0,
                    new Size(300, 300),
                    new Scalar(104, 177, 123),
                    false,
                    false
            );

            // 2. 输入网络
            faceNet.setInput(blob);
            // 3. 前向推理
            Mat detections = faceNet.forward();
            int faceCount = 0;
            // 4. 解析结果
            Mat detectionMat = detections.reshape(1, detections.size(2));
            for (int i = 0; i < detectionMat.rows(); i++) {
                double confidence = detectionMat.get(i, 2)[0];
                if (confidence > 0.5) {
                    faceCount++;
                }
            }
            System.out.println("检测到人脸个数：" + faceCount);
            return faceCount;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("人脸检测失败: " + e.getMessage());
            return 0;
        }
    }
}
