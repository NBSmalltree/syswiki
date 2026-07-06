package com.syswiki.rag;

import com.syswiki.model.vo.ContentVO;
import com.syswiki.service.ContentService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内容检索器
 * 负责在指定 systemId 的向量空间中检索与用户问题最相关的知识片段。
 *
 * 主路径：向量语义检索（精确、高效）
 * 降级路径：数据库关键词匹配（当向量检索结果不足时自动回退）
 */
@Component
public class SysWikiContentRetriever {

    private static final Logger log = LoggerFactory.getLogger(SysWikiContentRetriever.class);

    private static final int MAX_RESULTS = 5;
    private static final int MIN_VECTOR_RESULTS = 2;
    private static final int FALLBACK_MAX_MODULES = 3;

    private final SysWikiEmbeddingStoreFactory storeFactory;
    private final EmbeddingModel embeddingModel;
    private final ContentService contentService;

    public SysWikiContentRetriever(SysWikiEmbeddingStoreFactory storeFactory, EmbeddingModel embeddingModel,
                                   ContentService contentService) {
        this.storeFactory = storeFactory;
        this.embeddingModel = embeddingModel;
        this.contentService = contentService;
    }

    /**
     * 检索与用户问题最相关的知识片段
     *
     * @param systemId 系统空间 ID
     * @param query 用户问题文本
     * @return 相关文本片段列表，按相似度降序排列
     */
    public List<String> retrieve(String systemId, String query) {
        // 1. 主路径：向量检索
        var queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = storeFactory.search(systemId, queryEmbedding, MAX_RESULTS);

        if (matches.size() >= MIN_VECTOR_RESULTS) {
            List<String> results = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.toList());
            log.info("向量检索命中: systemId={}, matches={}, query={}",
                systemId, results.size(), truncate(query, 50));
            return results;
        }

        // 2. 降级路径：数据库关键词匹配
        log.info("向量结果不足 ({} < {})，触发数据库降级: systemId={}, query={}",
            matches.size(), MIN_VECTOR_RESULTS, systemId, truncate(query, 50));

        return fallbackRetrieve(systemId, query);
    }

    /**
     * 降级检索：基于数据库内容的简单关键词匹配
     * 将用户问题拆分为关键词，统计各模块命中数，返回匹配度最高的模块内容
     */
    private List<String> fallbackRetrieve(String systemId, String query) {
        try {
            List<ContentVO> allContents = contentService.listContents(systemId);
            if (allContents == null || allContents.isEmpty()) {
                log.info("数据库降级无内容: systemId={}", systemId);
                return List.of();
            }

            // 提取关键词：按非中文字符/空白拆分，过滤常用停用词和过短词
            Set<String> keywords = extractKeywords(query);

            if (keywords.isEmpty()) {
                // 无法提取关键词，返回前 N 个模块的简短摘要
                return allContents.stream()
                    .limit(FALLBACK_MAX_MODULES)
                    .map(c -> "【" + c.getModuleType() + "】\n" + truncate(c.getMdContent(), 200))
                    .collect(Collectors.toList());
            }

            // 对每个模块计算关键词匹配得分
            List<ModuleScore> scored = new ArrayList<>();
            for (ContentVO content : allContents) {
                if (content.getMdContent() == null || content.getMdContent().isEmpty()) continue;
                String lowerContent = content.getMdContent().toLowerCase();
                int hitCount = 0;
                for (String kw : keywords) {
                    if (lowerContent.contains(kw)) hitCount++;
                }
                if (hitCount > 0) {
                    scored.add(new ModuleScore(content, hitCount));
                }
            }

            // 按命中数降序，取 Top N
            scored.sort((a, b) -> Integer.compare(b.score, a.score));
            List<ContentVO> topModules = scored.stream()
                .limit(FALLBACK_MAX_MODULES)
                .map(s -> s.content)
                .collect(Collectors.toList());

            if (topModules.isEmpty()) {
                log.info("数据库降级无匹配: systemId={}, keywords={}", systemId, keywords);
                return List.of();
            }

            List<String> results = topModules.stream()
                .map(c -> "【" + c.getModuleType() + "】\n" + c.getMdContent())
                .collect(Collectors.toList());

            log.info("数据库降级命中: systemId={}, modules={}, keywords={}",
                systemId, topModules.stream().map(ContentVO::getModuleType).collect(Collectors.toList()), keywords);
            return results;

        } catch (Exception e) {
            log.error("数据库降级检索异常: systemId={}", systemId, e);
            return List.of();
        }
    }

    /**
     * 从用户问题中提取关键词
     * 拆分规则：按空白/标点拆分，保留中文词（2+字）和英文词（2+字符，过滤无意义词）。
     * 对较长的中文词生成 2-字滑动窗口，增强匹配覆盖。
     */
    private Set<String> extractKeywords(String query) {
        if (query == null || query.trim().isEmpty()) return Set.of();

        String lower = query.toLowerCase();
        // 按非字母/非中文拆分
        String[] parts = lower.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        Set<String> keywords = new LinkedHashSet<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            boolean isChinese = trimmed.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
            if (isChinese) {
                // 中文：保留 2+ 字的词，且不是停用词
                // 对 4+ 字的长词生成 2-字滑动窗口，如 "服务器配置" → "服务器" + "器配置"
                if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                    keywords.add(trimmed);
                }
                if (trimmed.length() >= 4) {
                    for (int i = 0; i <= trimmed.length() - 2; i++) {
                        String sub = trimmed.substring(i, i + 2);
                        if (!STOP_WORDS.contains(sub)) {
                            keywords.add(sub);
                        }
                    }
                }
            } else {
                if (trimmed.length() >= 2) {
                    keywords.add(trimmed);
                }
            }
        }
        return keywords;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private static class ModuleScore {
        final ContentVO content;
        final int score;
        ModuleScore(ContentVO content, int score) {
            this.content = content;
            this.score = score;
        }
    }

    /** 常用中文停用词 */
    private static final Set<String> STOP_WORDS = Set.of(
        "什么", "怎么", "如何", "哪里", "为什么", "这个", "那个", "这些", "那些",
        "一个", "可以", "没有", "不是", "就是", "但是", "还是", "因为", "所以",
        "如果", "虽然", "而且", "或者", "关于", "对于", "按照", "通过", "根据",
        "系统", "使用", "进行", "需要", "提供", "包括", "以及", "所有", "以下",
        "介绍", "说明", "描述", "定义", "配置", "设置", "功能"
    );
}
