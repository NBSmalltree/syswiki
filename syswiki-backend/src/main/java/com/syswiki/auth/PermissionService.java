package com.syswiki.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.model.entity.SysSystemMember;
import com.syswiki.mapper.SysSystemMemberMapper;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {
    private final SysSystemMemberMapper memberMapper;

    public PermissionService(SysSystemMemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public boolean isAdmin(String role) { return "ADMIN".equals(role); }

    public boolean canEditSystem(String userId, String role, String systemId) {
        if ("ADMIN".equals(role)) return true;
        if (!"EDITOR".equals(role)) return false;
        LambdaQueryWrapper<SysSystemMember> w = new LambdaQueryWrapper<>();
        w.eq(SysSystemMember::getSystemId, systemId)
         .eq(SysSystemMember::getUserId, userId)
         .in(SysSystemMember::getRole, "OWNER", "ADMIN");
        return memberMapper.selectCount(w) > 0;
    }

    public void requireEditPermission(String userId, String role, String systemId) {
        if (!canEditSystem(userId, role, systemId))
            throw new BizException(ErrorCode.FORBIDDEN, "无权编辑此系统");
    }

    public void requireAdmin(String role) {
        if (!isAdmin(role))
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员权限");
    }
}
