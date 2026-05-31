package com.syswiki.controller;

import com.syswiki.auth.PermissionService;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.UserVO;
import com.syswiki.service.UserService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final PermissionService permissionService;

    public UserController(UserService userService, PermissionService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Result<List<UserVO>> list() {
        List<UserVO> users = userService.listUsers();
        return Result.success(users);
    }

    @GetMapping("/{userId}")
    public Result<UserVO> detail(@PathVariable String userId) {
        return Result.success(userService.getUser(userId));
    }

    @PutMapping("/{userId}/role")
    public Result<Void> updateRole(@PathVariable String userId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        permissionService.requireAdmin((String) request.getAttribute("currentRole"));
        userService.updateRole(userId, body.get("role"));
        return Result.success(null);
    }

    @PutMapping("/{userId}/disable")
    public Result<Void> disable(@PathVariable String userId, HttpServletRequest request) {
        permissionService.requireAdmin((String) request.getAttribute("currentRole"));
        userService.disableUser(userId);
        return Result.success(null);
    }
}
