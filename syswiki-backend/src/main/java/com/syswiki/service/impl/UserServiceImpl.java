package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.auth.JwtUtil;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysLoginLogMapper;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.dto.LoginDTO;
import com.syswiki.model.dto.RegisterDTO;
import com.syswiki.model.entity.SysLoginLog;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.model.vo.UserVO;
import com.syswiki.service.UserService;
import com.syswiki.util.BeanConverter;
import com.syswiki.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;
    private final SysLoginLogMapper loginLogMapper;

    public UserServiceImpl(JwtUtil jwtUtil, SysLoginLogMapper loginLogMapper) {
        this.jwtUtil = jwtUtil;
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public TokenVO login(LoginDTO dto, String ip) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getUsername, dto.getUsername());
        SysUser user = getOne(w);

        String logStatus = "SUCCESS";
        String logMsg = "登录成功";

        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            logStatus = "FAIL";
            logMsg = "用户名或密码错误";
            log.warn("用户登录失败: username={}, ip={}, reason={}", dto.getUsername(), ip, logMsg);
            saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);
            throw new BizException(ErrorCode.AUTH_FAILED, logMsg);
        }
        if ("DISABLED".equals(user.getStatus())) {
            logStatus = "FAIL";
            logMsg = "账号已禁用";
            log.warn("用户登录失败: username={}, ip={}, reason={}", dto.getUsername(), ip, logMsg);
            saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);
            throw new BizException(ErrorCode.AUTH_FAILED, logMsg);
        }

        log.info("用户登录成功: username={}, ip={}", dto.getUsername(), ip);
        saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        return BeanConverter.toTokenVO(user, token);
    }

    @Override
    public TokenVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getUsername, dto.getUsername());
        if (count(w) > 0) {
            log.warn("用户注册失败: username={}, reason=用户名已存在", dto.getUsername());
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUserId(IdGenerator.nextId("U"));
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        // 新注册用户默认VIEWER，由管理员提升角色
        user.setRole("VIEWER");
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        save(user);

        log.info("用户注册成功: username={}, userId={}", user.getUsername(), user.getUserId());
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        return BeanConverter.toTokenVO(user, token);
    }

    @Override
    public List<UserVO> listUsers() {
        return BeanConverter.toUserVOList(list());
    }

    @Override
    public UserVO getUser(String userId) {
        SysUser u = getById(userId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        return BeanConverter.toUserVO(u);
    }

    @Override
    public void updateRole(String userId, String role) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if ("admin".equals(user.getUsername())) throw new BizException(ErrorCode.FORBIDDEN, "不可修改管理员角色");
        user.setRole(role);
        updateById(user);
        log.info("用户角色变更: userId={}, newRole={}", userId, role);
    }

    @Override
    public void disableUser(String userId) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if ("admin".equals(user.getUsername())) throw new BizException(ErrorCode.FORBIDDEN, "不可禁用管理员");
        user.setStatus("DISABLED");
        updateById(user);
        log.info("用户已禁用: userId={}, username={}", userId, user.getUsername());
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_FAILED, "旧密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新密码长度不能少于6位");
        }
        user.setPassword(encoder.encode(newPassword));
        updateById(user);
        log.info("密码修改成功: userId={}", userId);
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新密码长度不能少于6位");
        }
        user.setPassword(encoder.encode(newPassword));
        updateById(user);
        log.info("密码重置成功: userId={}", userId);
    }

    private void saveLoginLog(String username, String ip, String status, String message) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setLogId(IdGenerator.nextId("LG"));
        loginLog.setUsername(username);
        loginLog.setLoginIp(ip);
        loginLog.setStatus(status);
        loginLog.setMessage(message);
        loginLog.setCreateTime(LocalDateTime.now());
        loginLogMapper.insert(loginLog);
    }
}
