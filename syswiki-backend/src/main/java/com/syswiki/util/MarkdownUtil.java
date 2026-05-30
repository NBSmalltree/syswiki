package com.syswiki.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown解析工具
 * 负责按标题层级拆分Markdown内容为多个模块
 */
public class MarkdownUtil {

    private static final Pattern H2_PATTERN = Pattern.compile("^##\\s+(.+)$");

    /**
     * 按二级标题拆分Markdown为模块Map
     * @return Map<moduleType, content>
     */
    public static Map<String, String> parseByH2(String markdown) {
        Map<String, String> result = new LinkedHashMap<>();
        if (markdown == null || markdown.isEmpty()) return result;

        String[] lines = markdown.split("\n");
        String currentModule = null;
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            Matcher m = H2_PATTERN.matcher(line.trim());
            if (m.matches()) {
                if (currentModule != null) {
                    result.put(currentModule, sb.toString().trim());
                }
                currentModule = matchModule(m.group(1).trim());
                sb = new StringBuilder();
            } else if (currentModule != null) {
                sb.append(line).append("\n");
            }
        }
        if (currentModule != null) {
            result.put(currentModule, sb.toString().trim());
        }
        return result;
    }

    private static String matchModule(String heading) {
        String h = heading.toLowerCase();
        if (h.contains("简介") || h.contains("技术栈")) return "INTRO";
        if (h.contains("测试") && h.contains("架构")) return "ARCH_TEST";
        if (h.contains("生产") && h.contains("架构")) return "ARCH_PROD";
        if (h.contains("服务器") || h.contains("基础设施")) return "SERVER";
        if (h.contains("网络") || h.contains("防火墙") || h.contains("负载")) return "NETWORK";
        if (h.contains("数据库") || h.contains("db")) return "DATABASE";
        if (h.contains("接入") || h.contains("指南")) return "GUIDE";
        return null;
    }

    /**
     * 合并多个模块为完整Markdown
     */
    public static String mergeModules(Map<String, String> modules) {
        StringBuilder sb = new StringBuilder();
        String[][] order = {
            {"INTRO", "系统简介与技术栈"},
            {"ARCH_TEST", "测试环境架构"},
            {"ARCH_PROD", "生产环境架构"},
            {"SERVER", "服务器配置"},
            {"NETWORK", "网络策略管理"},
            {"DATABASE", "数据库配置"},
            {"GUIDE", "快速接入指南"}
        };
        for (String[] pair : order) {
            String content = modules.get(pair[0]);
            if (content != null && !content.isEmpty()) {
                sb.append("## ").append(pair[1]).append("\n\n");
                sb.append(content).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
