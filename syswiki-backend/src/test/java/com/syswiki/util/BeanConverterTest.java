package com.syswiki.util;

import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.model.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BeanConverter 工具类单元测试")
class BeanConverterTest {

    private SysUser buildUser() {
        SysUser user = new SysUser();
        user.setUserId("U001");
        user.setUsername("testuser");
        user.setPassword("hashed_password");
        user.setNickname("测试用户");
        user.setRole("VIEWER");
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.of(2025, 1, 15, 10, 30, 0));
        user.setUpdateTime(LocalDateTime.of(2025, 6, 1, 14, 0, 0));
        return user;
    }

    private SysEncySpace buildSpace() {
        SysEncySpace space = new SysEncySpace();
        space.setSystemId("SP001");
        space.setSystemName("测试系统");
        space.setSystemCode("TEST_SYS");
        space.setOwner("admin");
        space.setDescription("测试系统描述");
        space.setStatus("ACTIVE");
        space.setCreateTime(LocalDateTime.of(2025, 1, 15, 10, 30, 0));
        space.setUpdateTime(LocalDateTime.of(2025, 6, 1, 14, 0, 0));
        return space;
    }

    // ==================== toUserVO 测试 ====================

    @Nested
    @DisplayName("toUserVO 方法")
    class ToUserVOTests {

        @Test
        @DisplayName("正常转换 - 所有字段正确映射")
        void toUserVO_normal() {
            // given
            SysUser user = buildUser();

            // when
            UserVO vo = BeanConverter.toUserVO(user);

            // then
            assertNotNull(vo);
            assertEquals("U001", vo.getUserId());
            assertEquals("testuser", vo.getUsername());
            assertEquals("测试用户", vo.getNickname());
            assertEquals("VIEWER", vo.getRole());
            assertEquals("ACTIVE", vo.getStatus());
            assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 0), vo.getCreateTime());
        }

        @Test
        @DisplayName("null输入 - 返回null")
        void toUserVO_nullInput() {
            // when
            UserVO vo = BeanConverter.toUserVO((SysUser) null);

            // then
            assertNull(vo);
        }

        @Test
        @DisplayName("带覆盖角色的重载方法 - 正常转换")
        void toUserVO_withOverrideRole() {
            // given
            SysUser user = buildUser();

            // when
            UserVO vo = BeanConverter.toUserVO(user, "ADMIN");

            // then
            assertNotNull(vo);
            assertEquals("ADMIN", vo.getRole());
            // 其他字段保持不变
            assertEquals("U001", vo.getUserId());
            assertEquals("testuser", vo.getUsername());
        }

        @Test
        @DisplayName("带覆盖角色的重载方法 - overrideRole为null时不覆盖")
        void toUserVO_withNullOverrideRole() {
            // given
            SysUser user = buildUser();

            // when
            UserVO vo = BeanConverter.toUserVO(user, null);

            // then
            assertNotNull(vo);
            assertEquals("VIEWER", vo.getRole());
        }

        @Test
        @DisplayName("带覆盖角色的重载方法 - user为null时返回null")
        void toUserVO_withOverrideRole_nullUser() {
            // when
            UserVO vo = BeanConverter.toUserVO(null, "ADMIN");

            // then
            assertNull(vo);
        }
    }

    // ==================== toUserVOList 测试 ====================

    @Nested
    @DisplayName("toUserVOList 方法")
    class ToUserVOListTests {

        @Test
        @DisplayName("正常转换用户列表")
        void toUserVOList_normal() {
            // given
            SysUser user1 = buildUser();
            SysUser user2 = new SysUser();
            user2.setUserId("U002");
            user2.setUsername("user2");
            user2.setNickname("用户二");
            user2.setRole("ADMIN");
            user2.setStatus("ACTIVE");

            // when
            List<UserVO> result = BeanConverter.toUserVOList(Arrays.asList(user1, user2));

            // then
            assertEquals(2, result.size());
            assertEquals("U001", result.get(0).getUserId());
            assertEquals("U002", result.get(1).getUserId());
            assertEquals("ADMIN", result.get(1).getRole());
        }

        @Test
        @DisplayName("null输入 - 返回空列表")
        void toUserVOList_nullInput() {
            // when
            List<UserVO> result = BeanConverter.toUserVOList(null);

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("空列表输入 - 返回空列表")
        void toUserVOList_emptyInput() {
            // when
            List<UserVO> result = BeanConverter.toUserVOList(Collections.emptyList());

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== toTokenVO 测试 ====================

    @Nested
    @DisplayName("toTokenVO 方法")
    class ToTokenVOTests {

        @Test
        @DisplayName("正常转换 - 所有字段正确映射")
        void toTokenVO_normal() {
            // given
            SysUser user = buildUser();

            // when
            TokenVO vo = BeanConverter.toTokenVO(user, "jwt-token-12345", "refresh-token-abc");

            // then
            assertNotNull(vo);
            assertEquals("jwt-token-12345", vo.getToken());
            assertEquals("refresh-token-abc", vo.getRefreshToken());
            assertEquals("testuser", vo.getUsername());
            assertEquals("测试用户", vo.getNickname());
            assertEquals("VIEWER", vo.getRole());
        }

        @Test
        @DisplayName("null user输入 - 返回null")
        void toTokenVO_nullUser() {
            // when
            TokenVO vo = BeanConverter.toTokenVO(null, "jwt-token-12345", "ref-abc");

            // then
            assertNull(vo);
        }

        @Test
        @DisplayName("token字段为null时 - token为null但其他字段正常")
        void toTokenVO_nullToken() {
            // given
            SysUser user = buildUser();

            // when
            TokenVO vo = BeanConverter.toTokenVO(user, null, "refresh-abc");

            // then
            assertNotNull(vo);
            assertNull(vo.getToken());
            assertEquals("refresh-abc", vo.getRefreshToken());
            assertEquals("testuser", vo.getUsername());
        }
    }

    // ==================== toSpaceVO 测试 ====================

    @Nested
    @DisplayName("toSpaceVO 方法")
    class ToSpaceVOTests {

        @Test
        @DisplayName("正常转换 - 所有字段正确映射")
        void toSpaceVO_normal() {
            // given
            SysEncySpace space = buildSpace();

            // when
            SpaceVO vo = BeanConverter.toSpaceVO(space);

            // then
            assertNotNull(vo);
            assertEquals("SP001", vo.getSystemId());
            assertEquals("测试系统", vo.getSystemName());
            assertEquals("TEST_SYS", vo.getSystemCode());
            assertEquals("admin", vo.getOwner());
            assertEquals("测试系统描述", vo.getDescription());
            assertEquals("ACTIVE", vo.getStatus());
            assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 0), vo.getCreateTime());
            assertEquals(LocalDateTime.of(2025, 6, 1, 14, 0, 0), vo.getUpdateTime());
        }

        @Test
        @DisplayName("null输入 - 返回null")
        void toSpaceVO_nullInput() {
            // when
            SpaceVO vo = BeanConverter.toSpaceVO(null);

            // then
            assertNull(vo);
        }

        @Test
        @DisplayName("部分字段为null时 - 正常转换不抛异常")
        void toSpaceVO_partialNull() {
            // given
            SysEncySpace space = new SysEncySpace();
            space.setSystemId("SP002");
            space.setSystemName("部分系统");
            space.setSystemCode("PARTIAL");
            // owner, description, status, createTime, updateTime 均为null

            // when
            SpaceVO vo = BeanConverter.toSpaceVO(space);

            // then
            assertNotNull(vo);
            assertEquals("SP002", vo.getSystemId());
            assertEquals("部分系统", vo.getSystemName());
            assertEquals("PARTIAL", vo.getSystemCode());
            assertNull(vo.getOwner());
            assertNull(vo.getDescription());
            assertNull(vo.getStatus());
            assertNull(vo.getCreateTime());
            assertNull(vo.getUpdateTime());
        }
    }
}
