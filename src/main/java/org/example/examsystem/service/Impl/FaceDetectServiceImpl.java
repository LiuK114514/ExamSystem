package org.example.examsystem.service.Impl;

import org.example.examsystem.service.IService.FaceDetectService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@ConditionalOnClass(name = "org.opencv.core.Mat")
public class FaceDetectServiceImpl implements FaceDetectService {

    private Class<?> netClass;
    private Class<?> dnnClass;
    private Class<?> imgcodecsClass;
    private Class<?> matClass;
    private Class<?> scalarClass;
    private Class<?> sizeClass;
    private Class<?> matOfByteClass;
    private Object faceNet;
    private boolean initialized = false;

    public FaceDetectServiceImpl() {
        try {
            netClass = Class.forName("org.opencv.dnn.Net");
            dnnClass = Class.forName("org.opencv.dnn.Dnn");
            imgcodecsClass = Class.forName("org.opencv.imgcodecs.Imgcodecs");
            matClass = Class.forName("org.opencv.core.Mat");
            scalarClass = Class.forName("org.opencv.core.Scalar");
            sizeClass = Class.forName("org.opencv.core.Size");
            matOfByteClass = Class.forName("org.opencv.core.MatOfByte");

            System.loadLibrary("opencv_java455");

            File protoFile = new File(
                    getClass().getResource("/dnn/deploy.prototxt.txt").toURI());
            File modelFile = new File(
                    getClass().getResource("/dnn/res10_300x300_ssd_iter_140000.caffemodel").toURI());

            faceNet = dnnClass.getMethod("readNetFromCaffe", String.class, String.class)
                    .invoke(null, protoFile.getAbsolutePath(), modelFile.getAbsolutePath());

            initialized = true;
        } catch (Throwable e) {
            System.out.println("OpenCV初始化失败，人脸检测功能将不可用: " + e.getMessage());
            initialized = false;
        }
    }

    @Override
    public int detectFace(String base64) {
        if (!initialized) {
            return 0;
        }
        try {
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }

            byte[] bytes = java.util.Base64.getDecoder().decode(base64);

            Object mat = imgcodecsClass.getMethod("imdecode", matOfByteClass, int.class)
                    .invoke(null, matOfByteClass.getConstructor(byte[].class).newInstance(bytes), 1);

            if ((Boolean) matClass.getMethod("empty").invoke(mat)) {
                return 0;
            }

            Object blob = dnnClass.getMethod("blobFromImage", matClass, double.class, sizeClass, scalarClass, boolean.class, boolean.class)
                    .invoke(null, mat, 1.0,
                            sizeClass.getConstructor(double.class, double.class).newInstance(300.0, 300.0),
                            scalarClass.getConstructor(double.class, double.class, double.class).newInstance(104.0, 177.0, 123.0),
                            false, false);

            netClass.getMethod("setInput", matClass).invoke(faceNet, blob);
            Object detections = netClass.getMethod("forward").invoke(faceNet);

            Object detectionMat = matClass.getMethod("reshape", int.class, int.class).invoke(detections, 1, matClass.getMethod("size", int.class).invoke(detections, 2));

            int faceCount = 0;
            int rows = (Integer) matClass.getMethod("rows").invoke(detectionMat);
            for (int i = 0; i < rows; i++) {
                Object result = matClass.getMethod("get", int.class, int.class).invoke(detectionMat, i, 2);
                double confidence = ((double[]) result)[0];
                if (confidence > 0.5) {
                    faceCount++;
                }
            }
            return faceCount;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}