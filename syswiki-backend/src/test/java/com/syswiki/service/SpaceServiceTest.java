package com.syswiki.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.exception.BizException;
import com.syswiki.mapper.*;
import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.entity.SysSystemMember;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.impl.SpaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceService 单元测试")
class SpaceServiceTest {

    @Spy
    @InjectMocks
    private SpaceServiceImpl spaceService;

    @Mock
    private SysSystemMemberMapper memberMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysEncyContentMapper contentMapper;

    @Mock
    private SysEncyContentVersionMapper versionMapper;

    @Mock
    private SysEncyTopologyMapper topologyMapper;

    @Mock
    private SysEncySqlLibMapper sqlLibMapper;

    private SysEncySpace buildSpace(String systemId, String systemName, String systemCode,
                                     String owner, String status) {
        SysEncySpace space = new SysEncySpace();
        space.setSystemId(systemId);
        space.setSystemName(systemName);
        space.setSystemCode(systemCode);
        space.setOwner(owner);
        space.setDescription("测试描述");
        space.setStatus(status);
        space.setCreateTime(LocalDateTime.now());
        space.setUpdateTime(LocalDateTime.now());
        return space;
    }

    private void mockOwnerResolution(String systemId, SysUser ownerUser) {
        SysSystemMember member = new SysSystemMember();
        member.setId("M001");
        member.setSystemId(systemId);
        member.setUserId(ownerUser.getUserId());
        member.setRole("ADMIN");

        when(memberMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(member));
        when(userMapper.selectById(ownerUser.getUserId())).thenReturn(ownerUser);
    }

    private SysUser buildUser(String userId, String username, String nickname, String role) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setRole(role);
        return user;
    }

    // ==================== createSpace 测试 ====================

    @Nested
    @DisplayName("createSpace 方法")
    class CreateSpaceTests {

        @Test
        @DisplayName("成功创建系统空间")
        void createSpace_success() {
            // given
            SpaceCreateDTO dto = new SpaceCreateDTO();
            dto.setSystemName("测试系统");
            dto.setSystemCode("TEST_SYS");
            dto.setOwner("admin");
            dto.setDescription("这是一个测试系统");

            doReturn(0L).when(spaceService).count(any(LambdaQueryWrapper.class));
            doReturn(true).when(spaceService).save(any(SysEncySpace.class));

            // 创建空间时 toVO 调用 resolveOwnerNickname，需要 mock
            // 不能使用 anyString() 与 raw value 混合，所以使用 lenient 方式内联 mock
            SysUser adminUser = buildUser("U001", "admin", "管理员", "ADMIN");
            SysSystemMember member = new SysSystemMember();
            member.setId("M001");
            member.setUserId("U001");
            member.setRole("ADMIN");
            when(memberMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(member));
            when(userMapper.selectById("U001")).thenReturn(adminUser);

            // when
            SpaceVO result = spaceService.createSpace(dto);

            // then
            assertNotNull(result);
            assertEquals("测试系统", result.getSystemName());
            assertEquals("TEST_SYS", result.getSystemCode());
            assertEquals("ACTIVE", result.getStatus());
            assertEquals("这是一个测试系统", result.getDescription());

            ArgumentCaptor<SysEncySpace> captor = ArgumentCaptor.forClass(SysEncySpace.class);
            verify(spaceService).save(captor.capture());
            SysEncySpace saved = captor.getValue();
            assertEquals("测试系统", saved.getSystemName());
            assertEquals("TEST_SYS", saved.getSystemCode());
            assertEquals("ACTIVE", saved.getStatus());
            assertNotNull(saved.getSystemId());
        }

        @Test
        @DisplayName("创建失败 - 系统代号已存在")
        void createSpace_duplicateCode() {
            // given
            SpaceCreateDTO dto = new SpaceCreateDTO();
            dto.setSystemName("测试系统");
            dto.setSystemCode("EXISTING_CODE");
            dto.setOwner("admin");

            doReturn(1L).when(spaceService).count(any(LambdaQueryWrapper.class));

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> spaceService.createSpace(dto));
            assertEquals(10002, ex.getCode());
        }
    }

    // ==================== listActiveSpaces 测试 ====================

    @Nested
    @DisplayName("listActiveSpaces 方法")
    class ListActiveSpacesTests {

        @Test
        @DisplayName("返回活跃系统列表")
        void listActiveSpaces_success() {
            // given
            SysEncySpace space1 = buildSpace("SP001", "系统A", "SYS_A", "admin", "ACTIVE");
            SysEncySpace space2 = buildSpace("SP002", "系统B", "SYS_B", "admin", "ACTIVE");

            doReturn(Arrays.asList(space1, space2)).when(spaceService).list(any(LambdaQueryWrapper.class));

            // mock owner resolution for each space
            SysUser adminUser = buildUser("U001", "admin", "管理员", "ADMIN");
            SysSystemMember member1 = new SysSystemMember();
            member1.setId("M001");
            member1.setSystemId("SP001");
            member1.setUserId("U001");
            SysSystemMember member2 = new SysSystemMember();
            member2.setId("M002");
            member2.setSystemId("SP002");
            member2.setUserId("U001");

            when(memberMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(member1))
                    .thenReturn(Collections.singletonList(member2));
            when(userMapper.selectById("U001")).thenReturn(adminUser);

            // when
            List<SpaceVO> result = spaceService.listActiveSpaces();

            // then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("系统A", result.get(0).getSystemName());
            assertEquals("系统B", result.get(1).getSystemName());
            verify(spaceService).list(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无活跃系统时返回空列表")
        void listActiveSpaces_empty() {
            // given
            doReturn(Collections.emptyList()).when(spaceService).list(any(LambdaQueryWrapper.class));

            // when
            List<SpaceVO> result = spaceService.listActiveSpaces();

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== listUserSpaces 测试 ====================

    @Nested
    @DisplayName("listUserSpaces 方法")
    class ListUserSpacesTests {

        @Test
        @DisplayName("返回用户有权限的系统列表")
        void listUserSpaces_success() {
            // given
            SysEncySpace space1 = buildSpace("SP001", "系统A", "SYS_A", "admin", "ACTIVE");

            // SpaceServiceImpl.listUserSpaces uses baseMapper.selectSpacesByUserId
            // We need to mock the baseMapper field. Since SpaceServiceImpl extends ServiceImpl,
            // the baseMapper is set by MyBatis-Plus. We can use ReflectionTestUtils or
            // mock via the spy pattern with doReturn.
            // Actually, for listUserSpaces it calls baseMapper.selectSpacesByUserId directly,
            // so we need to set up the baseMapper. Let's use a different approach.
            // Since spaceService is a @Spy, we can't easily mock baseMapper. Instead, we'll
            // use reflection to set it.

            SysEncySpaceMapper mockSpaceMapper = mock(SysEncySpaceMapper.class);
            try {
                java.lang.reflect.Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                        .getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(spaceService, mockSpaceMapper);
            } catch (Exception e) {
                fail("Failed to set baseMapper via reflection: " + e.getMessage());
            }

            when(mockSpaceMapper.selectSpacesByUserId("U001"))
                    .thenReturn(Collections.singletonList(space1));

            // mock owner resolution
            SysUser adminUser = buildUser("U001", "admin", "管理员", "ADMIN");
            mockOwnerResolution("SP001", adminUser);

            // when
            List<SpaceVO> result = spaceService.listUserSpaces("U001");

            // then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("系统A", result.get(0).getSystemName());
            verify(mockSpaceMapper).selectSpacesByUserId("U001");
        }

        @Test
        @DisplayName("用户无权限系统时返回空列表")
        void listUserSpaces_empty() {
            // given
            SysEncySpaceMapper mockSpaceMapper = mock(SysEncySpaceMapper.class);
            try {
                java.lang.reflect.Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                        .getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(spaceService, mockSpaceMapper);
            } catch (Exception e) {
                fail("Failed to set baseMapper via reflection: " + e.getMessage());
            }

            when(mockSpaceMapper.selectSpacesByUserId("U999"))
                    .thenReturn(Collections.emptyList());

            // when
            List<SpaceVO> result = spaceService.listUserSpaces("U999");

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 其他方法测试 ====================

    @Nested
    @DisplayName("其他SpaceService方法")
    class OtherSpaceTests {

        @Test
        @DisplayName("getSpaceDetail - 系统存在时返回详情")
        void getSpaceDetail_success() {
            // given
            SysEncySpace space = buildSpace("SP001", "系统A", "SYS_A", "admin", "ACTIVE");
            doReturn(space).when(spaceService).getById("SP001");

            SysUser adminUser = buildUser("U001", "admin", "管理员", "ADMIN");
            mockOwnerResolution("SP001", adminUser);

            // when
            SpaceVO result = spaceService.getSpaceDetail("SP001");

            // then
            assertNotNull(result);
            assertEquals("SP001", result.getSystemId());
            assertEquals("系统A", result.getSystemName());
        }

        @Test
        @DisplayName("getSpaceDetail - 系统不存在时抛出异常")
        void getSpaceDetail_notFound() {
            // given
            doReturn(null).when(spaceService).getById("SP999");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> spaceService.getSpaceDetail("SP999"));
            assertEquals(10001, ex.getCode());
        }

        @Test
        @DisplayName("validateSpaceExists - 系统存在时不抛异常")
        void validateSpaceExists_found() {
            // given
            SysEncySpace space = buildSpace("SP001", "系统A", "SYS_A", "admin", "ACTIVE");
            doReturn(space).when(spaceService).getById("SP001");

            // when & then
            assertDoesNotThrow(() -> spaceService.validateSpaceExists("SP001"));
        }

        @Test
        @DisplayName("validateSpaceExists - 系统不存在时抛出异常")
        void validateSpaceExists_notFound() {
            // given
            doReturn(null).when(spaceService).getById("SP999");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> spaceService.validateSpaceExists("SP999"));
            assertEquals(10001, ex.getCode());
        }
    }
}
