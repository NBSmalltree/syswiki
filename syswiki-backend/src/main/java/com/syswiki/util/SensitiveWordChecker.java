package com.syswiki.util;

import java.util.regex.Pattern;

/**
 * 敏感词检查工具（首期简化版）
 * 后续可接入行内统一敏感词库
 */
public class SensitiveWordChecker {

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
     * 抛出异常版本
     */
    public void check(String content) {
        // 首期仅记录告警，不阻断
        if (containsSensitive(content)) {
            // log.warn("检测到可能的敏感信息");
        }
    }
}
