package com.syswiki.service;

import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysEncyTopologyMapper;
import com.syswiki.model.dto.TopologySaveDTO;
import com.syswiki.model.entity.SysEncyTopology;
import com.syswiki.model.vo.TopologyVO;
import com.syswiki.service.impl.TopologyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("TopologyService 单元测试")
class TopologyServiceTest {

    private TopologyServiceImpl topologyService;

    @Mock
    private SysEncyTopologyMapper topologyMapper;

    @Mock
    private SpaceService spaceService;

    private static final String SYSTEM_ID = "SYS001";
    private static final String LINK_ID = "TL001";

    @BeforeEach
    void setUp() throws Exception {
        // 手动创建实例并注入 Mock 依赖
        topologyService = spy(new TopologyServiceImpl(spaceService));
        // 反射注入 MyBatis-Plus ServiceImpl 的 baseMapper 字段
        Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                .getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(topologyService, topologyMapper);
    }

    @Nested
    @DisplayName("updateTopology() 方法")
    class UpdateTopology {

        @Test
        @DisplayName("拓扑存在且属于目标系统时，应更新成功")
        void shouldUpdateWhenExistsAndBelongsToSystem() {
            SysEncyTopology existing = new SysEncyTopology();
            existing.setLinkId(LINK_ID);
            existing.setSystemId(SYSTEM_ID);
            existing.setFromNode("A");
            existing.setToNode("B");
            existing.setProtocol("HTTP");
            doReturn(existing).when(topologyService).getById(LINK_ID);
            doReturn(true).when(topologyService).updateById(any());

            TopologySaveDTO dto = new TopologySaveDTO();
            dto.setFromNode("A");
            dto.setToNode("C");
            dto.setProtocol("MQ");
            dto.setInterfaceName("notify");
            dto.setInterfaceDetails("updated");

            TopologyVO result = topologyService.updateTopology(SYSTEM_ID, LINK_ID, dto);

            assertEquals("C", result.getToNode());
            assertEquals("MQ", result.getProtocol());
            assertEquals("notify", result.getInterfaceName());
            assertEquals("updated", result.getInterfaceDetails());
        }

        @Test
        @DisplayName("拓扑不存在时，应抛出 NOT_FOUND 异常")
        void shouldThrowWhenNotFound() {
            doReturn(null).when(topologyService).getById(LINK_ID);
            TopologySaveDTO dto = new TopologySaveDTO();
            dto.setFromNode("A"); dto.setToNode("B");

            assertThrows(BizException.class,
                    () -> topologyService.updateTopology(SYSTEM_ID, LINK_ID, dto));
        }

        @Test
        @DisplayName("应拒绝自环更新")
        void shouldRejectSelfLoop() {
            SysEncyTopology existing = new SysEncyTopology();
            existing.setLinkId(LINK_ID);
            existing.setSystemId(SYSTEM_ID);
            doReturn(existing).when(topologyService).getById(LINK_ID);

            TopologySaveDTO dto = new TopologySaveDTO();
            dto.setFromNode("A"); dto.setToNode("A");

            assertThrows(BizException.class,
                    () -> topologyService.updateTopology(SYSTEM_ID, LINK_ID, dto));
        }
    }

    @Nested
    @DisplayName("deleteTopology() 方法")
    class DeleteTopology {

        @Test
        @DisplayName("拓扑存在且属于目标系统时，应删除成功")
        void shouldDeleteWhenExistsAndBelongsToSystem() {
            SysEncyTopology existing = new SysEncyTopology();
            existing.setLinkId(LINK_ID);
            existing.setSystemId(SYSTEM_ID);
            doReturn(existing).when(topologyService).getById(LINK_ID);
            doReturn(true).when(topologyService).removeById(LINK_ID);

            topologyService.deleteTopology(SYSTEM_ID, LINK_ID);

            verify(topologyService).removeById(LINK_ID);
        }

        @Test
        @DisplayName("拓扑不存在时，应抛出 NOT_FOUND 异常")
        void shouldThrowWhenTopologyNotFound() {
            doReturn(null).when(topologyService).getById(LINK_ID);

            BizException ex = assertThrows(BizException.class,
                    () -> topologyService.deleteTopology(SYSTEM_ID, LINK_ID));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(topologyService, never()).removeById(anyString());
        }

        @Test
        @DisplayName("拓扑属于其他系统时，应抛出 NOT_FOUND 异常")
        void shouldThrowWhenTopologyBelongsToOtherSystem() {
            SysEncyTopology existing = new SysEncyTopology();
            existing.setLinkId(LINK_ID);
            existing.setSystemId("OTHER_SYSTEM");
            doReturn(existing).when(topologyService).getById(LINK_ID);

            BizException ex = assertThrows(BizException.class,
                    () -> topologyService.deleteTopology(SYSTEM_ID, LINK_ID));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
            verify(topologyService, never()).removeById(anyString());
        }
    }

    @Nested
    @DisplayName("listTopologies() 方法")
    class ListTopologies {

        @Test
        @DisplayName("应验证系统空间存在")
        void shouldValidateSpaceExists() {
            when(topologyMapper.selectList(any())).thenReturn(Collections.emptyList());
            topologyService.listTopologies(SYSTEM_ID);
            verify(spaceService).validateSpaceExists(SYSTEM_ID);
        }

        @Test
        @DisplayName("应返回排序后的拓扑 VO 列表")
        void shouldReturnSortedTopologyVOs() {
            SysEncyTopology entity1 = new SysEncyTopology();
            entity1.setLinkId("TL001");
            entity1.setSystemId(SYSTEM_ID);
            entity1.setFromNode("A");
            entity1.setToNode("B");
            entity1.setProtocol("HTTP");
            entity1.setInterfaceName("query");
            entity1.setInterfaceDetails("req: id\\nres: name");
            entity1.setSortOrder(1);

            SysEncyTopology entity2 = new SysEncyTopology();
            entity2.setLinkId("TL002");
            entity2.setSystemId(SYSTEM_ID);
            entity2.setFromNode("B");
            entity2.setToNode("C");
            entity2.setProtocol("MQ");
            entity2.setInterfaceName("notify");
            entity2.setInterfaceDetails("");
            entity2.setSortOrder(0);

            // 模拟数据库按 sortOrder ASC 返回
            when(topologyMapper.selectList(any())).thenReturn(List.of(entity2, entity1));

            List<TopologyVO> result = topologyService.listTopologies(SYSTEM_ID);

            // 验证结果数量
            assertEquals(2, result.size());
            // 验证字段映射
            TopologyVO vo1 = result.get(0);
            assertEquals("TL002", vo1.getLinkId());
            assertEquals(SYSTEM_ID, vo1.getSystemId());
            assertEquals("B", vo1.getFromNode());
            assertEquals("C", vo1.getToNode());
            assertEquals("MQ", vo1.getProtocol());
            assertEquals("notify", vo1.getInterfaceName());
            assertEquals("", vo1.getInterfaceDetails());
            assertEquals(0, vo1.getSortOrder());

            TopologyVO vo2 = result.get(1);
            assertEquals("TL001", vo2.getLinkId());
            assertEquals("HTTP", vo2.getProtocol());
            assertEquals(1, vo2.getSortOrder());

            // 验证查询条件：systemId 过滤 + sortOrder 排序
            ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(topologyMapper).selectList(captor.capture());
            LambdaQueryWrapper<SysEncyTopology> actualQuery = captor.getValue();
            assertNotNull(actualQuery);
        }
    }

    @Nested
    @DisplayName("batchSave() 方法")
    class BatchSave {

        @Test
        @DisplayName("应验证系统空间存在")
        void shouldValidateSpaceExists() {
            doReturn(true).when(topologyService).remove(any());
            doReturn(true).when(topologyService).saveBatch(anyList());
            topologyService.batchSave(SYSTEM_ID, Collections.emptyList());
            verify(spaceService).validateSpaceExists(SYSTEM_ID);
        }

        @Test
        @DisplayName("应正确将 DTO 映射为 Entity，生成 ID 和排序号")
        void shouldMapDTOsToEntitiesWithIdAndSortOrder() {
            TopologySaveDTO dto1 = new TopologySaveDTO();
            dto1.setFromNode("A");
            dto1.setToNode("B");
            dto1.setProtocol("HTTP");
            dto1.setInterfaceName("query");
            dto1.setInterfaceDetails("请求：交易ID\n响应：状态码");

            TopologySaveDTO dto2 = new TopologySaveDTO();
            dto2.setFromNode("B");
            dto2.setToNode("C");
            dto2.setProtocol("MQ");
            dto2.setInterfaceName("notify");
            dto2.setInterfaceDetails("");

            List<SysEncyTopology> capturedEntities = new ArrayList<>();
            doReturn(true).when(topologyService).remove(any());
            doAnswer(invocation -> {
                List<SysEncyTopology> entities = invocation.getArgument(0);
                capturedEntities.addAll(entities);
                return true;
            }).when(topologyService).saveBatch(anyList());

            List<TopologyVO> result = topologyService.batchSave(SYSTEM_ID, List.of(dto1, dto2));

            // 验证旧数据已被删除
            ArgumentCaptor<LambdaQueryWrapper> removeCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(topologyService).remove(removeCaptor.capture());

            // 验证 Entity 映射
            assertEquals(2, capturedEntities.size());

            SysEncyTopology entity1 = capturedEntities.get(0);
            assertTrue(entity1.getLinkId().startsWith("TL"), "ID 应以 TL 开头");
            assertEquals(SYSTEM_ID, entity1.getSystemId());
            assertEquals("A", entity1.getFromNode());
            assertEquals("B", entity1.getToNode());
            assertEquals("HTTP", entity1.getProtocol());
            assertEquals("query", entity1.getInterfaceName());
            assertEquals("请求：交易ID\n响应：状态码", entity1.getInterfaceDetails());
            assertEquals(0, entity1.getSortOrder());
            assertNotNull(entity1.getCreateTime());
            assertNotNull(entity1.getUpdateTime());

            SysEncyTopology entity2 = capturedEntities.get(1);
            assertTrue(entity2.getLinkId().startsWith("TL"));
            assertEquals("B", entity2.getFromNode());
            assertEquals("C", entity2.getToNode());
            assertEquals("MQ", entity2.getProtocol());
            assertEquals("notify", entity2.getInterfaceName());
            assertEquals("", entity2.getInterfaceDetails());
            assertEquals(1, entity2.getSortOrder());

            // 验证返回的 VO 映射
            assertEquals(2, result.size());

            TopologyVO vo1 = result.get(0);
            assertEquals(entity1.getLinkId(), vo1.getLinkId());
            assertEquals(SYSTEM_ID, vo1.getSystemId());
            assertEquals("A", vo1.getFromNode());
            assertEquals("HTTP", vo1.getProtocol());
            assertEquals(0, vo1.getSortOrder());

            TopologyVO vo2 = result.get(1);
            assertEquals(entity2.getLinkId(), vo2.getLinkId());
            assertEquals("MQ", vo2.getProtocol());
            assertEquals(1, vo2.getSortOrder());

            // 验证两个 VO 的 linkId 不同
            assertNotEquals(vo1.getLinkId(), vo2.getLinkId());
        }

        @Test
        @DisplayName("应拒绝自环连接（fromNode == toNode）")
        void shouldRejectSelfLoop() {
            TopologySaveDTO dto = new TopologySaveDTO();
            dto.setFromNode("A");
            dto.setToNode("A");
            dto.setProtocol("HTTP");

            doReturn(true).when(topologyService).remove(any());

            BizException ex = assertThrows(BizException.class,
                    () -> topologyService.batchSave(SYSTEM_ID, List.of(dto)));
            assertTrue(ex.getMessage().contains("不能相同"));
        }

        @Test
        @DisplayName("应拒绝重复链路（相同 fromNode + toNode + protocol）")
        void shouldRejectDuplicateLink() {
            TopologySaveDTO dto1 = new TopologySaveDTO();
            dto1.setFromNode("A");
            dto1.setToNode("B");
            dto1.setProtocol("HTTP");

            TopologySaveDTO dto2 = new TopologySaveDTO();
            dto2.setFromNode("A");
            dto2.setToNode("B");
            dto2.setProtocol("HTTP");

            doReturn(true).when(topologyService).remove(any());

            BizException ex = assertThrows(BizException.class,
                    () -> topologyService.batchSave(SYSTEM_ID, List.of(dto1, dto2)));
            assertTrue(ex.getMessage().contains("重复"));
        }
    }
}
