package com.syswiki.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @GetMapping("/markdown-template.md")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            ClassPathResource res = new ClassPathResource("templates/markdown-template.md");
            InputStream is = res.getInputStream();
            byte[] bytes = is.readAllBytes();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=syswiki-template.md")
                .contentType(MediaType.TEXT_PLAIN).body(bytes);
        } catch (Exception e) {
            String fallback = "# 系统百科标准模版\n\n## 系统简介\n\n## 技术栈\n\n## 测试环境架构\n\n## 生产环境架构\n\n## 服务器配置\n\n## 网络策略\n\n## 数据库配置\n\n## 快速接入指南\n";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=syswiki-template.md")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fallback.getBytes(StandardCharsets.UTF_8));
        }
    }
}
