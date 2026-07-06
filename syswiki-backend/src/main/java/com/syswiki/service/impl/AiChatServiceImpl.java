package com.syswiki.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.rag.PromptTemplates;
import com.syswiki.rag.StreamCallback;
import com.syswiki.rag.SysWikiContentRetriever;
import com.syswiki.service.AiChatService;
import com.syswiki.service.SpaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final SpaceService spaceService;
    private final SysWikiContentRetriever contentRetriever;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${syswiki.ai.base-url}") private String aiBaseUrl;
    @Value("${syswiki.ai.api-key}") private String aiApiKey;
    @Value("${syswiki.ai.flash-model}") private String flashModel;
    @Value("${syswiki.ai.think-model}") private String thinkModel;

    public AiChatServiceImpl(SpaceService spaceService, SysWikiContentRetriever contentRetriever) {
        this.spaceService = spaceService;
        this.contentRetriever = contentRetriever;
        this.restTemplate = new RestTemplate();
        // 配置超时：连接 10s，读取 60s（适配 SSE 流式响应）
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate.setRequestFactory(factory);
    }

    @PostConstruct
    public void validateConfig() {
        if (aiBaseUrl == null || aiBaseUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                "AI_BASE_URL 环境变量未设置！请在启动前配置该变量。"
                + " 示例：export AI_BASE_URL=https://api.openai.com/v1");
        }
        if (aiApiKey == null || aiApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "AI_API_KEY 环境变量未设置！请在启动前配置该变量。"
                + " 示例：export AI_API_KEY=your-ai-api-key");
        }
    }

    @Override
    public void chat(String systemId, String message, String modelType, StreamCallback callback) {
        try {
            SysEncySpace space = spaceService.getById(systemId);
            if (space == null) { callback.onError(new BizException(ErrorCode.SPACE_NOT_FOUND)); return; }

            // 使用 RAG 检索：只获取与问题最相关的知识片段
            List<String> relevantChunks = contentRetriever.retrieve(systemId, message);
            String knowledge = relevantChunks.isEmpty()
                ? "（暂无相关知识片段）"
                : relevantChunks.stream()
                    .collect(Collectors.joining("\n\n---\n\n"));

            log.info("RAG检索结果: systemId={}, chunks={}, query={}",
                systemId, relevantChunks.size(), truncate(message, 50));

            String systemPrompt = PromptTemplates.buildSystemPrompt(space.getSystemName(), space.getOwner(), knowledge);
            if ("think".equals(modelType)) systemPrompt += "\n\n" + PromptTemplates.THINK_ADDITION;

            String modelName = "think".equals(modelType) ? thinkModel : flashModel;
            ObjectNode req = objectMapper.createObjectNode();
            req.put("model", modelName);
            req.put("stream", true);
            ArrayNode messages = req.putArray("messages");
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", message);

            String url = aiBaseUrl + "/chat/completions";
            restTemplate.execute(url, HttpMethod.POST,
                requestCallback -> {
                    requestCallback.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    requestCallback.getHeaders().setBearerAuth(aiApiKey);
                    requestCallback.getBody().write(objectMapper.writeValueAsBytes(req));
                },
                response -> {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            if ("[DONE]".equals(json)) { callback.onComplete(); return null; }
                            try {
                                JsonNode node = objectMapper.readTree(json);
                                JsonNode choices = node.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    JsonNode contentNode = delta.get("content");
                                    if (contentNode != null && !contentNode.isNull()) {
                                        String token = contentNode.asText();
                                        if (token != null && !token.isEmpty()) callback.onToken(token);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    callback.onComplete();
                    return null;
                });
        } catch (Exception e) {
            log.error("AI调用异常", e);
            callback.onError(e);
        }
    }

    private String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
