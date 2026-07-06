package com.syswiki.rag;

import com.syswiki.model.vo.ContentVO;
import com.syswiki.service.ContentService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysWikiContentRetriever 降级检索测试")
class SysWikiContentRetrieverTest {

    @InjectMocks
    private SysWikiContentRetriever retriever;

    @Mock
    private SysWikiEmbeddingStoreFactory storeFactory;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ContentService contentService;

    private final String systemId = "SP20260530000001";

    @Nested
    @DisplayName("向量检索主路径")
    class VectorSearchTests {

        @Test
        @DisplayName("向量结果充足时返回向量结果，不触发降级")
        void vector_results_sufficient_no_fallback() {
            // given
            Embedding queryEmb = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            when(embeddingModel.embed(anyString())).thenReturn(
                new dev.langchain4j.model.output.Response<>(queryEmb));
            TextSegment seg1 = TextSegment.from("这是关于系统架构的描述");
            TextSegment seg2 = TextSegment.from("服务器配置详细说明");
            TextSegment seg3 = TextSegment.from("数据库连接信息");
            List<EmbeddingMatch<TextSegment>> matches = List.of(
                new EmbeddingMatch<>(0.95, "id1", queryEmb, seg1),
                new EmbeddingMatch<>(0.90, "id2", queryEmb, seg2),
                new EmbeddingMatch<>(0.85, "id3", queryEmb, seg3)
            );
            when(storeFactory.search(eq(systemId), any(), eq(5))).thenReturn(matches);

            // when
            List<String> result = retriever.retrieve(systemId, "系统架构和服务器");

            // then: 返回 3 个结果，且 contentService 从未被调用（未触发降级）
            assertEquals(3, result.size());
            assertEquals("这是关于系统架构的描述", result.get(0));
            verify(contentService, never()).listContents(anyString());
        }

        @Test
        @DisplayName("向量结果不足时触发数据库降级")
        void vector_results_insufficient_should_fallback() {
            // given
            Embedding queryEmb = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            when(embeddingModel.embed(anyString())).thenReturn(
                new dev.langchain4j.model.output.Response<>(queryEmb));
            // 仅返回 1 个向量结果（低于 MIN_VECTOR_RESULTS=2）
            TextSegment seg1 = TextSegment.from("测试内容");
            List<EmbeddingMatch<TextSegment>> matches = List.of(
                new EmbeddingMatch<>(0.90, "id1", queryEmb, seg1));
            when(storeFactory.search(eq(systemId), any(), eq(5))).thenReturn(matches);

            // 数据库有内容
            ContentVO intro = new ContentVO();
            intro.setModuleType("INTRO");
            intro.setMdContent("# 系统简介\n\n本系统使用 Java 和 Spring Boot 开发。");
            ContentVO server = new ContentVO();
            server.setModuleType("SERVER");
            server.setMdContent("# 服务器配置\n\nCPU: 4核, 内存: 8GB");
            when(contentService.listContents(systemId)).thenReturn(List.of(intro, server));

            // when
            List<String> result = retriever.retrieve(systemId, "Java Spring Boot 服务器");

            // then: 触发了数据库降级，匹配到包含关键词的模块
            assertFalse(result.isEmpty());
            verify(contentService, times(1)).listContents(systemId);
            // INTRO 模块匹配"Java"，SERVER 模块匹配"服务器"
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("数据库降级路径")
    class FallbackSearchTests {

        @Test
        @DisplayName("向量无结果时降级到数据库，按关键词匹配")
        void fallback_keyword_matching() {
            // given: 向量无结果
            Embedding queryEmb = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            when(embeddingModel.embed(anyString())).thenReturn(
                new dev.langchain4j.model.output.Response<>(queryEmb));
            when(storeFactory.search(eq(systemId), any(), eq(5))).thenReturn(List.of());

            ContentVO server = new ContentVO();
            server.setModuleType("SERVER");
            server.setMdContent("# 服务器\nIP: 10.0.0.1, CPU: 4核");
            ContentVO db = new ContentVO();
            db.setModuleType("DATABASE");
            db.setMdContent("# 数据库\nMySQL 8.0, 主备架构");
            ContentVO network = new ContentVO();
            network.setModuleType("NETWORK");
            network.setMdContent("# 网络\n防火墙策略已开通");

            when(contentService.listContents(systemId)).thenReturn(List.of(server, db, network));

            // when: 搜索"服务器配置"（含 n-gram "服务器"、"配置"）
            List<String> result = retriever.retrieve(systemId, "服务器配置");

            // then: 通过 n-gram "服务器" 匹配到 SERVER 模块
            assertEquals(1, result.size());
            assertTrue(result.get(0).contains("SERVER"));
            assertTrue(result.get(0).contains("10.0.0.1"));
        }

        @Test
        @DisplayName("降级时无内容返回空列表")
        void fallback_no_content() {
            // given
            Embedding queryEmb = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            when(embeddingModel.embed(anyString())).thenReturn(
                new dev.langchain4j.model.output.Response<>(queryEmb));
            when(storeFactory.search(eq(systemId), any(), eq(5))).thenReturn(List.of());
            when(contentService.listContents(systemId)).thenReturn(List.of());

            // when
            List<String> result = retriever.retrieve(systemId, "任何问题");

            // then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("关键词匹配：中英文混合")
        void fallback_chinese_english_keywords() {
            // given
            Embedding queryEmb = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});
            when(embeddingModel.embed(anyString())).thenReturn(
                new dev.langchain4j.model.output.Response<>(queryEmb));
            when(storeFactory.search(eq(systemId), any(), eq(5))).thenReturn(List.of());

            ContentVO tech = new ContentVO();
            tech.setModuleType("INTRO");
            tech.setMdContent("技术栈：Java 11, Spring Boot 2.7, MySQL");
            ContentVO guide = new ContentVO();
            guide.setModuleType("GUIDE");
            guide.setMdContent("接入指南：配置 Maven 依赖");

            when(contentService.listContents(systemId)).thenReturn(List.of(tech, guide));

            // when: 英文词 "Spring Boot" 应拆分为 "spring" 和 "boot" 各自匹配
            List<String> result = retriever.retrieve(systemId, "Spring Boot 版本");

            // then
            assertFalse(result.isEmpty());
            assertTrue(result.get(0).contains("INTRO"));
        }
    }
}
