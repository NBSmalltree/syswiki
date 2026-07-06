package com.syswiki.service;

import com.syswiki.rag.SysWikiEmbeddingStoreFactory;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量同步服务
 * 负责将 Markdown 内容异步向量化并存入向量库
 *
 * 内置指数退避重试机制，应对 Embedding API 临时故障。
 * 所有方法均为异步执行，不影响主事务。
 */
@Service
public class VectorSyncService {

    private static final Logger log = LoggerFactory.getLogger(VectorSyncService.class);

    private final SysWikiEmbeddingStoreFactory storeFactory;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter textSplitter;

    private final int retryMaxAttempts;
    private final long retryInitialBackoff;
    private final double retryMultiplier;
    private final long retryMaxBackoff;

    public VectorSyncService(SysWikiEmbeddingStoreFactory storeFactory,
                             EmbeddingModel embeddingModel,
                             DocumentSplitter textSplitter,
                             @Value("${syswiki.async.retry-max-attempts:3}") int retryMaxAttempts,
                             @Value("${syswiki.async.retry-initial-backoff:1000}") long retryInitialBackoff,
                             @Value("${syswiki.async.retry-multiplier:2.0}") double retryMultiplier,
                             @Value("${syswiki.async.retry-max-backoff:10000}") long retryMaxBackoff) {
        this.storeFactory = storeFactory;
        this.embeddingModel = embeddingModel;
        this.textSplitter = textSplitter;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryInitialBackoff = retryInitialBackoff;
        this.retryMultiplier = retryMultiplier;
        this.retryMaxBackoff = retryMaxBackoff;
    }

    /**
     * 异步同步单个模块内容到向量库
     * 当 Markdown 内容更新时调用
     */
    @Async
    public void syncContent(String systemId, String moduleType, String mdContent) {
        embedAndStoreWithRetry(systemId, moduleType, mdContent);
    }

    /**
     * 全量重建指定系统的向量空间
     * 用于系统初始化或数据修复
     * 注意：不要在 @Async 方法内部调用同类 @Async 方法，Spring 代理不会拦截自调用
     */
    @Async
    public void rebuildAll(String systemId, List<String> allContents, List<String> moduleTypes) {
        log.info("开始全量重建向量: systemId={}, modules={}", systemId, moduleTypes.size());

        // 1. 清除旧向量
        storeFactory.clearSystem(systemId);

        // 2. 重新向量化所有内容（直接调用嵌入逻辑，避免 @Async 自调用问题）
        for (int i = 0; i < allContents.size(); i++) {
            embedAndStoreWithRetry(systemId, moduleTypes.get(i), allContents.get(i));
        }

        log.info("全量重建完成: systemId={}, modules={}", systemId, moduleTypes.size());
    }

    /**
     * 带指数退避重试的嵌入与存储
     * 临时故障（网络超时、服务不可用等）会自动重试
     */
    private void embedAndStoreWithRetry(String systemId, String moduleType, String mdContent) {
        long backoff = retryInitialBackoff;

        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                embedAndStore(systemId, moduleType, mdContent);
                return; // 成功则返回
            } catch (Exception e) {
                boolean isLastAttempt = (attempt == retryMaxAttempts);
                if (isLastAttempt) {
                    log.error("嵌入失败（已重试 {} 次）: systemId={}, module={}",
                        attempt - 1, systemId, moduleType, e);
                } else {
                    log.warn("嵌入失败（第 {}/{} 次），{}ms 后重试: systemId={}, module={}",
                        attempt, retryMaxAttempts, backoff, systemId, moduleType);
                    log.debug("异常详情", e);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断: systemId={}, module={}", systemId, moduleType);
                        return;
                    }
                    backoff = (long) Math.min(backoff * retryMultiplier, retryMaxBackoff);
                }
            }
        }
    }

    /**
     * 嵌入并存储单个模块内容到向量库（同步执行，不重试）
     */
    private void embedAndStore(String systemId, String moduleType, String mdContent) {
        log.debug("嵌入模块: systemId={}, module={}", systemId, moduleType);

        Document document = Document.from(mdContent);
        List<TextSegment> segments = textSplitter.split(document);

        for (TextSegment segment : segments) {
            segment.metadata().put("systemId", systemId);
            segment.metadata().put("moduleType", moduleType);
        }

        List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(segments).content();
        for (int i = 0; i < segments.size(); i++) {
            storeFactory.add(systemId, embeddings.get(i), segments.get(i));
        }

        storeFactory.incrementDocumentCount(systemId);

        log.debug("嵌入完成: systemId={}, module={}, chunks={}", systemId, moduleType, segments.size());
    }
}
