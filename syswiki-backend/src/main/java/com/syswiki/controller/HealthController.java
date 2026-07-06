package com.syswiki.controller;

import com.syswiki.model.vo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${spring.application.name:syswiki-backend}")
    private String appName;

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of(
            "status", "UP",
            "app", appName,
            "time", LocalDateTime.now().toString()
        ));
    }
}
