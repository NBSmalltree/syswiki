package com.syswiki.controller;

import com.syswiki.model.dto.LoginDTO;
import com.syswiki.model.dto.RegisterDTO;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.service.UserService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }

    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return Result.success(userService.login(dto, ip));
    }

    @PostMapping("/register")
    public Result<TokenVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }
}
