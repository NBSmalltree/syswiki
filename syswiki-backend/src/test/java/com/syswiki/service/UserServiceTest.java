package com.syswiki.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.auth.JwtUtil;
import com.syswiki.exception.BizException;
import com.syswiki.mapper.SysLoginLogMapper;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.dto.LoginDTO;
import com.syswiki.model.dto.RegisterDTO;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.service.impl.UserServiceImpl;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SysLoginLogMapper loginLogMapper;

    @Mock
    private SysUserMapper userMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private SysUser buildUser(String userId, String username, String rawPassword,
                               String nickname, String role, String status) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(status);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    // ==================== login 测试 ====================

    @Nested
    @DisplayName("login 方法")
    class LoginTests {

        @Test
        @DisplayName("成功登录 - 返回TokenVO")
        void login_success() {
            // given
            SysUser user = buildUser("U001", "testuser", "Pass123456",
                    "测试用户", "VIEWER", "ACTIVE");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("Pass123456");

            doReturn(user).when(userService).getOne(any(LambdaQueryWrapper.class));
            when(jwtUtil.generateToken("U001", "testuser", "VIEWER")).thenReturn("mock-jwt-token");
            when(loginLogMapper.insert(any())).thenReturn(1);

            // when
            TokenVO result = userService.login(dto, "127.0.0.1");

            // then
            assertNotNull(result);
            assertEquals("mock-jwt-token", result.getToken());
            assertEquals("testuser", result.getUsername());
            assertEquals("测试用户", result.getNickname());
            assertEquals("VIEWER", result.getRole());
            verify(loginLogMapper, atLeastOnce()).insert(any());
        }

        @Test
        @DisplayName("登录失败 - 用户名不存在")
        void login_userNotFound() {
            // given
            LoginDTO dto = new LoginDTO();
            dto.setUsername("nonexistent");
            dto.setPassword("Pass123456");

            doReturn(null).when(userService).getOne(any(LambdaQueryWrapper.class));
            when(loginLogMapper.insert(any())).thenReturn(1);

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.login(dto, "127.0.0.1"));
            assertEquals(10009, ex.getCode());
            assertEquals("用户名或密码错误", ex.getMessage());
            verify(loginLogMapper, atLeastOnce()).insert(any());
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_wrongPassword() {
            // given
            SysUser user = buildUser("U001", "testuser", "CorrectPass123",
                    "测试用户", "VIEWER", "ACTIVE");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("WrongPass123");

            doReturn(user).when(userService).getOne(any(LambdaQueryWrapper.class));
            when(loginLogMapper.insert(any())).thenReturn(1);

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.login(dto, "127.0.0.1"));
            assertEquals(10009, ex.getCode());
            assertEquals("用户名或密码错误", ex.getMessage());
            verify(loginLogMapper, atLeastOnce()).insert(any());
        }

        @Test
        @DisplayName("登录失败 - 账号被禁用")
        void login_userDisabled() {
            // given
            SysUser user = buildUser("U001", "testuser", "Pass123456",
                    "测试用户", "VIEWER", "DISABLED");

            LoginDTO dto = new LoginDTO();
            dto.setUsername("testuser");
            dto.setPassword("Pass123456");

            doReturn(user).when(userService).getOne(any(LambdaQueryWrapper.class));
            when(loginLogMapper.insert(any())).thenReturn(1);

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.login(dto, "127.0.0.1"));
            assertEquals(10009, ex.getCode());
            assertEquals("账号已禁用", ex.getMessage());
            verify(loginLogMapper, atLeastOnce()).insert(any());
        }
    }

    // ==================== register 测试 ====================

    @Nested
    @DisplayName("register 方法")
    class RegisterTests {

        @Test
        @DisplayName("成功注册 - 返回TokenVO")
        void register_success() {
            // given
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("Pass123456");
            dto.setNickname("新用户");

            doReturn(0L).when(userService).count(any(LambdaQueryWrapper.class));
            doReturn(true).when(userService).save(any(SysUser.class));
            when(jwtUtil.generateToken(anyString(), eq("newuser"), eq("VIEWER")))
                    .thenReturn("mock-jwt-token");

            // when
            TokenVO result = userService.register(dto);

            // then
            assertNotNull(result);
            assertEquals("mock-jwt-token", result.getToken());
            assertEquals("newuser", result.getUsername());
            assertEquals("新用户", result.getNickname());
            assertEquals("VIEWER", result.getRole());

            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).save(captor.capture());
            SysUser savedUser = captor.getValue();
            assertEquals("newuser", savedUser.getUsername());
            assertEquals("VIEWER", savedUser.getRole());
            assertEquals("ACTIVE", savedUser.getStatus());
            assertEquals("新用户", savedUser.getNickname());
            assertTrue(encoder.matches("Pass123456", savedUser.getPassword()));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_usernameExists() {
            // given
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("existinguser");
            dto.setPassword("Pass123456");

            doReturn(1L).when(userService).count(any(LambdaQueryWrapper.class));

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.register(dto));
            assertEquals(400, ex.getCode());
            assertEquals("用户名已存在", ex.getMessage());
        }

        @Test
        @DisplayName("注册 - nickname为null时使用username作为默认值")
        void register_defaultNickname() {
            // given
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("Pass123456");
            dto.setNickname(null);

            doReturn(0L).when(userService).count(any(LambdaQueryWrapper.class));
            doReturn(true).when(userService).save(any(SysUser.class));
            when(jwtUtil.generateToken(anyString(), eq("newuser"), eq("VIEWER")))
                    .thenReturn("mock-jwt-token");

            // when
            TokenVO result = userService.register(dto);

            // then
            assertNotNull(result);
            assertEquals("newuser", result.getNickname());

            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).save(captor.capture());
            assertEquals("newuser", captor.getValue().getNickname());
        }
    }

    // ==================== changePassword 测试 ====================

    @Nested
    @DisplayName("changePassword 方法")
    class ChangePasswordTests {

        @Test
        @DisplayName("成功修改密码")
        void changePassword_success() {
            // given
            SysUser user = buildUser("U001", "testuser", "OldPass123",
                    "测试用户", "VIEWER", "ACTIVE");

            doReturn(user).when(userService).getById("U001");
            doReturn(true).when(userService).updateById(any(SysUser.class));

            // when
            userService.changePassword("U001", "OldPass123", "NewPass123");

            // then
            ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).updateById(captor.capture());
            assertTrue(encoder.matches("NewPass123", captor.getValue().getPassword()));
        }

        @Test
        @DisplayName("修改密码失败 - 旧密码错误")
        void changePassword_wrongOldPassword() {
            // given
            SysUser user = buildUser("U001", "testuser", "OldPass123",
                    "测试用户", "VIEWER", "ACTIVE");

            doReturn(user).when(userService).getById("U001");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.changePassword("U001", "WrongOldPass", "NewPass123"));
            assertEquals(10009, ex.getCode());
            assertEquals("旧密码不正确", ex.getMessage());
        }

        @Test
        @DisplayName("修改密码失败 - 用户不存在")
        void changePassword_userNotFound() {
            // given
            doReturn(null).when(userService).getById("U999");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.changePassword("U999", "OldPass123", "NewPass123"));
            assertEquals(404, ex.getCode());
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("修改密码失败 - 新密码长度不足")
        void changePassword_newPasswordTooShort() {
            // given
            SysUser user = buildUser("U001", "testuser", "OldPass123",
                    "测试用户", "VIEWER", "ACTIVE");

            doReturn(user).when(userService).getById("U001");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.changePassword("U001", "OldPass123", "12345"));
            assertEquals(400, ex.getCode());
            assertEquals("新密码长度不能少于6位", ex.getMessage());
        }

        @Test
        @DisplayName("修改密码失败 - 新密码为null")
        void changePassword_newPasswordNull() {
            // given
            SysUser user = buildUser("U001", "testuser", "OldPass123",
                    "测试用户", "VIEWER", "ACTIVE");

            doReturn(user).when(userService).getById("U001");

            // when & then
            BizException ex = assertThrows(BizException.class,
                    () -> userService.changePassword("U001", "OldPass123", null));
            assertEquals(400, ex.getCode());
            assertEquals("新密码长度不能少于6位", ex.getMessage());
        }
    }
}
