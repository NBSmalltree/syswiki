package com.syswiki.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 向量存储工厂
 * 每个 systemId 维护独立的向量存储空间，实现多系统隔离
 *
 * 首期方案：使用 LangChain4j 内置的 InProcessEmbeddingStore（内存存储）
 * 后续可替换为 PgVector 或 Qdrant，仅需修改此类
 */
@Component
public class SysWikiEmbeddingStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(SysWikiEmbeddingStoreFactory.class);

    /**
     * 按 systemId 隔离的向量存储映射
     */
    private final ConcurrentHashMap<String, EmbeddingStore<TextSegment>> stores = new ConcurrentHashMap<>();

    /**
     * 每个系统的向量存储状态追踪
     */
    private final ConcurrentHashMap<String, SystemVectorStatus> statusMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定系统的向量存储
     */
    public EmbeddingStore<TextSegment> getStore(String systemId) {
        return stores.computeIfAbsent(systemId, id -> {
            log.info("创建向量存储: systemId={}", id);
            return new InMemoryEmbeddingStore<>();
        });
    }

    /**
     * 添加向量到指定系统空间
     */
    public void add(String systemId, Embedding embedding, TextSegment segment) {
        getStore(systemId).add(embedding, segment);
        SystemVectorStatus status = statusMap.computeIfAbsent(systemId, k -> new SystemVectorStatus());
        status.chunkCount.incrementAndGet();
        status.lastSyncTime = LocalDateTime.now();
    }

    /**
     * 从指定系统空间检索相似内容
     *
     * @param systemId 系统空间 ID
     * @param queryEmbedding 查询向量
     * @param maxResults 最大返回结果数
     * @return 匹配的片段列表，按相似度降序排列
     */
    public List<EmbeddingMatch<TextSegment>> search(String systemId, Embedding queryEmbedding, int maxResults) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(0.6)  // 相似度阈值
            .build();

        EmbeddingSearchResult<TextSegment> result = getStore(systemId).search(request);
        return result.matches();
    }

    /**
     * 清除指定系统的全部向量（用于全量重建）
     */
    public void clearSystem(String systemId) {
        stores.remove(systemId);
        statusMap.remove(systemId);
        log.info("清除向量存储: systemId={}", systemId);
    }

    /**
     * 增加指定系统的文档计数
     */
    public void incrementDocumentCount(String systemId) {
        SystemVectorStatus status = statusMap.computeIfAbsent(systemId, k -> new SystemVectorStatus());
        status.documentCount.incrementAndGet();
    }

    /**
     * 获取指定系统的向量存储状态
     */
    public Map<String, Object> getStatus(String systemId) {
        SystemVectorStatus status = statusMap.get(systemId);
        if (status == null) {
            return Map.of(
                "status", "EMPTY",
                "totalDocuments", 0,
                "totalChunks", 0,
                "lastSyncTime", null
            );
        }
        return Map.of(
            "status", "READY",
            "totalDocuments", status.documentCount.get(),
            "totalChunks", status.chunkCount.get(),
            "lastSyncTime", status.lastSyncTime != null ? status.lastSyncTime.toString() : null
        );
    }

    /**
     * 系统向量存储状态
     */
    private static class SystemVectorStatus {
        private final AtomicInteger documentCount = new AtomicInteger(0);
        private final AtomicInteger chunkCount = new AtomicInteger(0);
        private volatile LocalDateTime lastSyncTime;
    }
}
