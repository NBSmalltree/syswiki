package com.syswiki.service;

import com.syswiki.rag.StreamCallback;

public interface AiChatService {
    void chat(String systemId, String message, String model, StreamCallback callback);
}
