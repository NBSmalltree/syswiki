package com.syswiki.controller;

import com.syswiki.auth.PermissionService;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.entity.SysUser;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.SpaceService;
import com.syswiki.service.SystemMemberService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
    private final SpaceService spaceService;
    private final PermissionService permissionService;
    private final SystemMemberService memberService;
    private final SysUserMapper userMapper;

    public SpaceController(SpaceService spaceService, PermissionService permissionService,
                           SystemMemberService memberService, SysUserMapper userMapper) {
        this.spaceService = spaceService;
        this.permissionService = permissionService;
        this.memberService = memberService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<List<SpaceVO>> list() { return Result.success(spaceService.listActiveSpaces()); }

    @GetMapping("/my")
    public Result<List<SpaceVO>> mySystems(HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        List<SpaceVO> all = spaceService.listActiveSpaces();
        if ("ADMIN".equals(role)) return Result.success(all);
        // EDITOR: 只返回自己是成员的系统
        List<SpaceVO> mine = all.stream()
            .filter(s -> permissionService.canEditSystem(userId, role, s.getSystemId()))
            .collect(java.util.stream.Collectors.toList());
        return Result.success(mine);
    }

    @GetMapping("/{systemId}")
    public Result<SpaceVO> detail(@PathVariable String systemId) { return Result.success(spaceService.getSpaceDetail(systemId)); }

    @GetMapping("/{systemId}/permission")
    public Result<Map<String, Object>> permission(@PathVariable String systemId, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        Map<String, Object> p = new HashMap<>();
        p.put("canEdit", permissionService.canEditSystem(userId, role, systemId));
        p.put("isAdmin", permissionService.isAdmin(role));
        return Result.success(p);
    }

    @PostMapping
    public Result<SpaceVO> create(@RequestBody @Valid SpaceCreateDTO dto, HttpServletRequest request) {
        String role = (String) request.getAttribute("currentRole");
        // ADMIN和EDITOR都可以创建系统
        if (!"ADMIN".equals(role) && !"EDITOR".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要EDITOR或ADMIN角色才能创建系统");
        }
        // 自动设置owner为当前用户
        String username = (String) request.getAttribute("currentUsername");
        dto.setOwner(username);
        SpaceVO space = spaceService.createSpace(dto);
        // 将创建者设为系统OWNER
        SysUser user = userMapper.selectById((String) request.getAttribute("currentUserId"));
        if (user != null) {
            memberService.addMember(space.getSystemId(), user.getUserId(), "OWNER");
        }
        return Result.success(space);
    }

    @PutMapping("/{systemId}")
    public Result<SpaceVO> update(@PathVariable String systemId, @RequestBody @Valid SpaceCreateDTO dto, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        return Result.success(spaceService.updateSpace(systemId, dto));
    }

    @DeleteMapping("/{systemId}")
    public Result<Void> delete(@PathVariable String systemId, HttpServletRequest request) {
        permissionService.requireAdmin((String) request.getAttribute("currentRole"));
        spaceService.deleteSpace(systemId);
        return Result.success(null);
    }
}
