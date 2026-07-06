package com.syswiki.service;

import com.syswiki.rag.SysWikiEmbeddingStoreFactory;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VectorSyncService 单元测试")
class VectorSyncServiceTest {

    private VectorSyncService vectorSyncService;

    @Mock
    private SysWikiEmbeddingStoreFactory storeFactory;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private DocumentSplitter textSplitter;

    // 重试参数默认值（与 application.yml 一致）
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private static final long RETRY_INITIAL_BACKOFF = 10;   // 测试中用 10ms
    private static final double RETRY_MULTIPLIER = 2.0;
    private static final long RETRY_MAX_BACKOFF = 100;

    private final String systemId = "SP20260530000001";
    private final String moduleType = "INTRO";
    private final String mdContent = "# 系统简介\n\n这是一个测试系统的描述。\n\n## 技术栈\n\n- Java 11\n- Spring Boot";

    @BeforeEach
    void setUp() {
        vectorSyncService = new VectorSyncService(
            storeFactory, embeddingModel, textSplitter,
            RETRY_MAX_ATTEMPTS, RETRY_INITIAL_BACKOFF, RETRY_MULTIPLIER, RETRY_MAX_BACKOFF);
    }

    @Nested
    @DisplayName("syncContent 方法")
    class SyncContentTests {

        @Test
        @DisplayName("成功向量化并存储切片")
        void syncContent_should_embed_and_store_segments() {
            // given
            TextSegment segment1 = TextSegment.from("片段1：系统简介");
            TextSegment segment2 = TextSegment.from("片段2：技术栈");

            when(textSplitter.split(any(Document.class))).thenReturn(List.of(segment1, segment2));

            Embedding embedding1 = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            Embedding embedding2 = Embedding.from(new float[]{0.4f, 0.5f, 0.6f});
            Response<List<Embedding>> response = new Response<>(List.of(embedding1, embedding2));
            when(embeddingModel.embedAll(anyList())).thenReturn(response);

            // when
            vectorSyncService.syncContent(systemId, moduleType, mdContent);

            // then
            verify(textSplitter).split(any(Document.class));
            verify(embeddingModel).embedAll(anyList());
            verify(storeFactory, times(2)).add(eq(systemId), any(Embedding.class), any(TextSegment.class));
            verify(storeFactory).incrementDocumentCount(systemId);

            // 验证片段元数据被设置
            ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
            verify(storeFactory, atLeastOnce()).add(eq(systemId), any(Embedding.class), segmentCaptor.capture());
            TextSegment captured = segmentCaptor.getValue();
            assertEquals(systemId, captured.metadata().get("systemId"));
            assertEquals(moduleType, captured.metadata().get("moduleType"));
        }

        @Test
        @DisplayName("临时故障后重试成功")
        void syncContent_retry_after_transient_failure() {
            // given
            TextSegment segment = TextSegment.from("测试内容");
            when(textSplitter.split(any(Document.class))).thenReturn(List.of(segment));

            Embedding embedding = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});

            // 第一次调用抛出异常，第二次调用成功
            when(embeddingModel.embedAll(anyList()))
                .thenThrow(new RuntimeException("网络超时"))
                .thenReturn(new Response<>(List.of(embedding)));

            // when
            vectorSyncService.syncContent(systemId, moduleType, mdContent);

            // then: embedAll 被调用了两次（首次失败 + 重试成功）
            verify(embeddingModel, times(2)).embedAll(anyList());

            // 最终存储成功
            verify(storeFactory).add(eq(systemId), any(Embedding.class), any(TextSegment.class));
            verify(storeFactory).incrementDocumentCount(systemId);
        }

        @Test
        @DisplayName("重试耗尽后记录最终错误，不抛异常")
        void syncContent_max_retries_exhausted_should_log_not_throw() {
            // given
            when(textSplitter.split(any(Document.class)))
                .thenThrow(new RuntimeException("服务不可用"));

            // when & then: 不抛异常（所有异常被 catch 记录）
            assertDoesNotThrow(() ->
                vectorSyncService.syncContent(systemId, moduleType, mdContent));

            // textSplitter.split 被调用了 3 次（初次 + 2次重试）
            verify(textSplitter, times(3)).split(any(Document.class));

            // 存储从未成功
            verify(storeFactory, never()).add(any(), any(), any());
        }

        @Test
        @DisplayName("内容为空时跳过向量化")
        void syncContent_empty_content_should_skip() {
            // when - 传入空内容
            vectorSyncService.syncContent(systemId, moduleType, "");

            // then
            verify(embeddingModel, never()).embedAll(anyList());
            verify(storeFactory, never()).add(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("rebuildAll 方法")
    class RebuildAllTests {

        @Test
        @DisplayName("全量重建：先清除再写入")
        void rebuildAll_should_clear_and_rebuild() {
            // given
            List<String> contents = List.of(
                "# 系统简介\n\n简介内容",
                "# 服务器配置\n\n服务器列表"
            );
            List<String> moduleTypes = List.of("INTRO", "SERVER");

            TextSegment segment1 = TextSegment.from("简介内容片段");
            TextSegment segment2 = TextSegment.from("服务器配置片段");

            when(textSplitter.split(any(Document.class))).thenReturn(List.of(segment1), List.of(segment2));

            Embedding embedding = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            Response<List<Embedding>> response = new Response<>(List.of(embedding));
            when(embeddingModel.embedAll(anyList())).thenReturn(response);

            // when
            vectorSyncService.rebuildAll(systemId, contents, moduleTypes);

            // then
            verify(storeFactory).clearSystem(systemId);
            verify(textSplitter, times(2)).split(any(Document.class));
            verify(embeddingModel, times(2)).embedAll(anyList());
            verify(storeFactory, times(2)).incrementDocumentCount(systemId);
        }
    }
}
