package com.syswiki.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 管理 Embedding 模型和文本切分器等 AI 相关 Bean
 */
@Configuration
public class LangChainConfig {

    @Value("${syswiki.ai.embedding-base-url}")
    private String embeddingBaseUrl;

    @Value("${syswiki.ai.embedding-api-key}")
    private String embeddingApiKey;

    @Value("${syswiki.ai.embedding-model}")
    private String embeddingModel;

    /**
     * Embedding 模型（用于将文本转为向量）
     * 使用 OpenAI 兼容协议，可对接 DeepSeek Embedding 或 OpenAI 等
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .baseUrl(embeddingBaseUrl)
            .apiKey(embeddingApiKey)
            .modelName(embeddingModel)
            .build();
    }

    /**
     * 文本切分器
     * 按段落切分，每段最大 500 字符，重叠 50 字符
     */
    @Bean
    public DocumentSplitter textSplitter() {
        return DocumentSplitters.recursive(500, 50);
    }
}
