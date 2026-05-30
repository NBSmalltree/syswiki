package com.syswiki.controller;

import com.syswiki.model.dto.AiChatRequest;
import com.syswiki.model.vo.AiChatChunk;
import com.syswiki.model.vo.Result;
import com.syswiki.rag.StreamCallback;
import com.syswiki.service.AiChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/spaces/{systemId}/ai")
public class AiController {
    private final AiChatService aiChatService;
    public AiController(AiChatService aiChatService) { this.aiChatService = aiChatService; }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable String systemId, @RequestBody @Valid AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);
        aiChatService.chat(systemId, request.getMessage(), request.getModel(), new StreamCallback() {
            @Override public void onToken(String token) {
                try { emitter.send(SseEmitter.event().data(new AiChatChunk(token, false), MediaType.APPLICATION_JSON)); }
                catch (IOException e) { emitter.completeWithError(e); }
            }
            @Override public void onComplete() {
                try { emitter.send(SseEmitter.event().data(new AiChatChunk("", true), MediaType.APPLICATION_JSON)); emitter.complete(); }
                catch (IOException e) { emitter.completeWithError(e); }
            }
            @Override public void onError(Throwable t) {
                try { emitter.send(SseEmitter.event().data(new AiChatChunk("AI异常: " + t.getMessage(), true), MediaType.APPLICATION_JSON)); }
                catch (IOException ignored) {}
                emitter.completeWithError(t);
            }
        });
        return emitter;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status(@PathVariable String systemId) {
        Map<String, Object> s = new HashMap<>();
        s.put("status", "READY");
        s.put("totalDocuments", 0);
        s.put("totalChunks", 0);
        return Result.success(s);
    }
}
