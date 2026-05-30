package com.syswiki.rag;

import java.util.List;

/**
 * 流式回调接口
 */
public interface StreamCallback {
    void onToken(String token);
    void onComplete();
    void onError(Throwable t);
}
