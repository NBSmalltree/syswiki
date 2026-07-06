package com.syswiki.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.exception.BizException;
import com.syswiki.mapper.SysEncyContentVersionMapper;
import com.syswiki.model.dto.ContentSaveDTO;
import com.syswiki.model.entity.SysEncyContent;
import com.syswiki.model.entity.SysEncyContentVersion;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.service.impl.ContentServiceImpl;
import com.syswiki.util.SensitiveWordChecker;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService 单元测试")
class ContentServiceTest {

    @Spy
    @InjectMocks
    private ContentServiceImpl contentService;

    @Mock
    private SpaceService spaceService;

    @Mock
    private MarkdownParserService markdownParserService;

    @Mock
    private SysEncyContentVersionMapper versionMapper;

    @Mock
    private VectorSyncService vectorSyncService;

    @Mock
    private SensitiveWordChecker sensitiveWordChecker;

    private final String systemId = "SP20260530000001";
    private final String moduleType = "INTRO";
    private final String mdContent = "# 系统简介\n\n测试内容。";

    @Nested
    @DisplayName("saveContent 方法")
    class SaveContentTests {

        @Test
        @DisplayName("更新已有内容 - 触发向量同步")
        void saveContent_update_existing_should_trigger_vector_sync() {
            // given
            SysEncyContent existing = new SysEncyContent();
            existing.setContentId("CT20260530000001");
            existing.setSystemId(systemId);
            existing.setModuleType(moduleType);
            existing.setMdContent("旧内容");
            existing.setVersion(1);
            existing.setOperator("user1");

            ContentSaveDTO dto = new ContentSaveDTO();
            dto.setMdContent(mdContent);
            dto.setOperator("user2");

            doReturn(existing).when(contentService).getOne(any(LambdaQueryWrapper.class));
            doReturn(true).when(contentService).updateById(any(SysEncyContent.class));

            // when
            ContentVO result = contentService.saveContent(systemId, moduleType, dto);

            // then
            assertNotNull(result);
            assertEquals(mdContent, result.getMdContent());
            assertEquals(2, result.getVersion());
            assertEquals("user2", result.getOperator());

            // 验证触发了向量同步
            verify(vectorSyncService).syncContent(systemId, moduleType, mdContent);

            // 验证保存了版本历史
            ArgumentCaptor<SysEncyContentVersion> captor = ArgumentCaptor.forClass(SysEncyContentVersion.class);
            verify(versionMapper).insert(captor.capture());
            assertEquals(1, captor.getValue().getVersion());
            assertEquals("旧内容", captor.getValue().getMdContent());
        }

        @Test
        @DisplayName("创建新内容 - 触发向量同步")
        void saveContent_new_content_should_trigger_vector_sync() {
            // given
            ContentSaveDTO dto = new ContentSaveDTO();
            dto.setMdContent(mdContent);
            dto.setOperator("admin");

            doReturn(null).when(contentService).getOne(any(LambdaQueryWrapper.class));
            doReturn(true).when(contentService).save(any(SysEncyContent.class));

            // when
            ContentVO result = contentService.saveContent(systemId, moduleType, dto);

            // then
            assertNotNull(result);
            assertEquals(mdContent, result.getMdContent());
            assertEquals(1, result.getVersion());
            assertEquals("admin", result.getOperator());

            // 验证触发了向量同步
            verify(vectorSyncService).syncContent(systemId, moduleType, mdContent);

            // 验证首次创建也保存了版本历史
            ArgumentCaptor<SysEncyContentVersion> captor = ArgumentCaptor.forClass(SysEncyContentVersion.class);
            verify(versionMapper).insert(captor.capture());
            assertEquals(1, captor.getValue().getVersion());
        }

        @Test
        @DisplayName("空间不存在时抛出异常")
        void saveContent_space_not_found_should_throw() {
            // given
            ContentSaveDTO dto = new ContentSaveDTO();
            dto.setMdContent(mdContent);
            dto.setOperator("admin");

            doThrow(new BizException(com.syswiki.exception.ErrorCode.SPACE_NOT_FOUND))
                .when(spaceService).validateSpaceExists(systemId);

            // when & then
            BizException ex = assertThrows(BizException.class,
                () -> contentService.saveContent(systemId, moduleType, dto));
            assertEquals(10001, ex.getCode());

            // 验证未触发向量同步
            verify(vectorSyncService, never()).syncContent(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getVersionHistory 方法")
    class GetVersionHistoryTests {

        @Test
        @DisplayName("有历史版本时返回版本列表")
        void getVersionHistory_has_versions() {
            // given
            SysEncyContentVersion v1 = new SysEncyContentVersion();
            v1.setVersionId("CV001");
            v1.setSystemId(systemId);
            v1.setModuleType(moduleType);
            v1.setVersion(2);
            v1.setMdContent("# v2 内容");
            v1.setOperator("user2");

            SysEncyContentVersion v2 = new SysEncyContentVersion();
            v2.setVersionId("CV002");
            v2.setSystemId(systemId);
            v2.setModuleType(moduleType);
            v2.setVersion(1);
            v2.setMdContent("# v1 内容");
            v2.setOperator("user1");

            when(versionMapper.selectList(any())).thenReturn(List.of(v1, v2));

            // when
            var result = contentService.getVersionHistory(systemId, moduleType);

            // then
            assertEquals(2, result.size());
            assertEquals(2, result.get(0).getVersion());
            assertEquals(1, result.get(1).getVersion());
        }

        @Test
        @DisplayName("无历史版本但有内容时自动补建")
        void getVersionHistory_auto_backfill() {
            // given
            SysEncyContent content = new SysEncyContent();
            content.setContentId("CT001");
            content.setSystemId(systemId);
            content.setModuleType(moduleType);
            content.setMdContent(mdContent);
            content.setVersion(1);
            content.setOperator("admin");

            when(versionMapper.selectList(any())).thenReturn(List.of());
            doReturn(content).when(contentService).getOne(any(LambdaQueryWrapper.class));

            // when
            var result = contentService.getVersionHistory(systemId, moduleType);

            // then
            verify(versionMapper, times(2)).selectList(any());
            ArgumentCaptor<SysEncyContentVersion> captor = ArgumentCaptor.forClass(SysEncyContentVersion.class);
            verify(versionMapper).insert(captor.capture());
            assertEquals(1, captor.getValue().getVersion());
        }
    }
}
