package com.syswiki.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.rag.PromptTemplates;
import com.syswiki.rag.StreamCallback;
import com.syswiki.service.AiChatService;
import com.syswiki.service.ContentService;
import com.syswiki.service.SpaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final SpaceService spaceService;
    private final ContentService contentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${syswiki.ai.base-url}") private String aiBaseUrl;
    @Value("${syswiki.ai.api-key}") private String aiApiKey;
    @Value("${syswiki.ai.flash-model}") private String flashModel;
    @Value("${syswiki.ai.think-model}") private String thinkModel;

    public AiChatServiceImpl(SpaceService spaceService, ContentService contentService) {
        this.spaceService = spaceService;
        this.contentService = contentService;
    }

    @Override
    public void chat(String systemId, String message, String modelType, StreamCallback callback) {
        try {
            SysEncySpace space = spaceService.getById(systemId);
            if (space == null) { callback.onError(new BizException(ErrorCode.SPACE_NOT_FOUND)); return; }

            List<ContentVO> contents = contentService.listContents(systemId);
            String knowledge = contents.stream()
                .filter(c -> c.getMdContent() != null && !c.getMdContent().isEmpty())
                .map(c -> "【" + c.getModuleType() + "】\n" + c.getMdContent())
                .collect(Collectors.joining("\n\n---\n\n"));

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
            RestTemplate restTemplate = new RestTemplate();
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
                                    if (delta != null && delta.has("content")) callback.onToken(delta.get("content").asText());
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
}
