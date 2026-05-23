package org.example.examsystem.controller;

import org.example.examsystem.service.IService.FaceDetectService;
import org.example.examsystem.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    @Autowired
    private FaceDetectService faceDetectService;

    @PostMapping("/checkFace")
    public Result checkFace(@RequestBody Map<String, String> req) {
        String base64 = req.get("image");
        int faceCount = faceDetectService.detectFace(base64);
        Map<String, Object> res = new HashMap<>();
        res.put("faceCount", faceCount);
        res.put("hasFace", faceCount > 0);
        res.put("status", faceCount == 0 ? "NO_FACE" :
                faceCount == 1 ? "NORMAL" : "MULTI_FACE");

        return Result.ok(res);
    }
}
