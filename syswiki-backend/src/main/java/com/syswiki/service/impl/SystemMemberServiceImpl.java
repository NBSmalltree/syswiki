package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysSystemMemberMapper;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.entity.SysSystemMember;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.UserVO;
import com.syswiki.service.SystemMemberService;
import com.syswiki.util.BeanConverter;
import com.syswiki.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SystemMemberServiceImpl extends ServiceImpl<SysSystemMemberMapper, SysSystemMember> implements SystemMemberService {
    private final SysUserMapper userMapper;

    public SystemMemberServiceImpl(SysUserMapper userMapper) { this.userMapper = userMapper; }

    @Override
    public List<UserVO> listMembers(String systemId) {
        LambdaQueryWrapper<SysSystemMember> w = new LambdaQueryWrapper<>();
        w.eq(SysSystemMember::getSystemId, systemId);
        return list(w).stream().map(m -> {
            SysUser u = userMapper.selectById(m.getUserId());
            if (u == null) return null;
            return BeanConverter.toUserVO(u, m.getRole()); // 系统角色覆盖用户全局角色
        }).filter(v -> v != null).collect(Collectors.toList());
    }

    @Override
    public void addMember(String systemId, String userId, String role) {
        // 检查是否已是成员
        LambdaQueryWrapper<SysSystemMember> w = new LambdaQueryWrapper<>();
        w.eq(SysSystemMember::getSystemId, systemId).eq(SysSystemMember::getUserId, userId);
        if (count(w) > 0) throw new BizException(ErrorCode.PARAM_INVALID, "该用户已是系统成员");

        // 检查目标用户是否存在且为EDITOR
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (!"EDITOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole()))
            throw new BizException(ErrorCode.FORBIDDEN, "只能添加EDITOR或ADMIN角色的用户为系统成员");

        SysSystemMember member = new SysSystemMember();
        member.setId(IdGenerator.nextId("SM"));
        member.setSystemId(systemId);
        member.setUserId(userId);
        member.setRole(role != null ? role : "ADMIN");
        member.setCreateTime(LocalDateTime.now());
        save(member);
    }

    @Override
    public void removeMember(String systemId, String userId) {
        LambdaQueryWrapper<SysSystemMember> w = new LambdaQueryWrapper<>();
        w.eq(SysSystemMember::getSystemId, systemId).eq(SysSystemMember::getUserId, userId);
        // 不能移除OWNER
        SysSystemMember m = getOne(w);
        if (m != null && "OWNER".equals(m.getRole())) throw new BizException(ErrorCode.FORBIDDEN, "不能移除系统所有者");
        remove(w);
    }
}
