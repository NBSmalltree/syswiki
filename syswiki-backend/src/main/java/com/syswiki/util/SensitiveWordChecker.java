package com.syswiki.util;

import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 敏感词检查工具（首期简化版）
 * 后续可接入行内统一敏感词库
 */
@Component
public class SensitiveWordChecker {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordChecker.class);

    // 简化的敏感词模式：生产环境密码、密钥等
    private static final Pattern[] PATTERNS = {
        Pattern.compile("(?i)password\\s*[:=]\\s*['\"]?\\S+"),
        Pattern.compile("(?i)secret\\s*[:=]\\s*['\"]?\\S+"),
        Pattern.compile("(?i)api[_-]?key\\s*[:=]\\s*['\"]?\\S+"),
        Pattern.compile("(?i)token\\s*[:=]\\s*['\"]?[A-Za-z0-9]{20,}"),
    };

    /**
     * 检查内容是否包含敏感信息
     * @return 如果包含敏感信息返回true
     */
    public boolean containsSensitive(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查内容，发现敏感词时记录告警并阻断保存
     * @throws BizException 当检测到敏感内容时抛出
     */
    public void check(String content) {
        if (containsSensitive(content)) {
            log.warn("检测到可能的敏感信息，已阻止保存");
            throw new BizException(ErrorCode.SENSITIVE_WORD_DETECTED,
                "内容中包含疑似密码/密钥等敏感信息，请移除后重试");
        }
    }
}
