package com.syswiki.model.vo;

public class AiChatChunk {
    private String content;
    private boolean done;

    public AiChatChunk() {}
    public AiChatChunk(String content, boolean done) {
        this.content = content;
        this.done = done;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
