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
import com.syswiki.util.IdGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {
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
            saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);
            throw new BizException(ErrorCode.AUTH_FAILED, logMsg);
        }
        if ("DISABLED".equals(user.getStatus())) {
            logStatus = "FAIL";
            logMsg = "账号已禁用";
            saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);
            throw new BizException(ErrorCode.AUTH_FAILED, logMsg);
        }

        saveLoginLog(dto.getUsername(), ip, logStatus, logMsg);

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        TokenVO vo = new TokenVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public TokenVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getUsername, dto.getUsername());
        if (count(w) > 0) throw new BizException(ErrorCode.PARAM_INVALID, "用户名已存在");

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

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
        TokenVO vo = new TokenVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public List<UserVO> listUsers() {
        return list().stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setUserId(u.getUserId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole());
            vo.setStatus(u.getStatus());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public UserVO getUser(String userId) {
        SysUser u = getById(userId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        UserVO vo = new UserVO();
        vo.setUserId(u.getUserId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }

    @Override
    public void updateRole(String userId, String role) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if ("admin".equals(user.getUsername())) throw new BizException(ErrorCode.FORBIDDEN, "不可修改管理员角色");
        user.setRole(role);
        updateById(user);
    }

    @Override
    public void disableUser(String userId) {
        SysUser user = getById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if ("admin".equals(user.getUsername())) throw new BizException(ErrorCode.FORBIDDEN, "不可禁用管理员");
        user.setStatus("DISABLED");
        updateById(user);
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
    }

    private void saveLoginLog(String username, String ip, String status, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setLogId(IdGenerator.nextId("LG"));
        log.setUsername(username);
        log.setLoginIp(ip);
        log.setStatus(status);
        log.setMessage(message);
        log.setCreateTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
