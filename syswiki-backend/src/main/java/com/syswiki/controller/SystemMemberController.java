package com.syswiki.controller;

import com.syswiki.auth.PermissionService;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.UserVO;
import com.syswiki.service.SystemMemberService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces/{systemId}/members")
public class SystemMemberController {
    private final SystemMemberService memberService;
    private final PermissionService permissionService;

    public SystemMemberController(SystemMemberService memberService, PermissionService permissionService) {
        this.memberService = memberService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Result<List<UserVO>> list(@PathVariable String systemId) {
        List<UserVO> members = memberService.listMembers(systemId);
        return Result.success(members);
    }

    @PostMapping
    public Result<Void> add(@PathVariable String systemId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        memberService.addMember(systemId, body.get("userId"), body.get("role"));
        return Result.success(null);
    }

    @DeleteMapping("/{memberUserId}")
    public Result<Void> remove(@PathVariable String systemId, @PathVariable String memberUserId, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        memberService.removeMember(systemId, memberUserId);
        return Result.success(null);
    }
}
