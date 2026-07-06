package com.syswiki.controller;

import com.syswiki.model.dto.LoginDTO;
import com.syswiki.model.dto.RegisterDTO;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.service.UserService;
import com.syswiki.util.LoginRateLimiter;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(UserService userService, LoginRateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        rateLimiter.check(ip);
        try {
            TokenVO token = userService.login(dto, ip);
            rateLimiter.clear(ip);
            return Result.success(token);
        } catch (Exception e) {
            rateLimiter.recordFailure(ip);
            throw e;
        }
    }

    @PostMapping("/register")
    public Result<TokenVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new com.syswiki.exception.BizException(
                com.syswiki.exception.ErrorCode.PARAM_INVALID, "refreshToken 不能为空");
        }
        return Result.success(userService.refresh(refreshToken));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }
}
