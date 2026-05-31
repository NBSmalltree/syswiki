package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysEncySpaceMapper;
import com.syswiki.mapper.SysSystemMemberMapper;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.entity.SysSystemMember;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.SpaceService;
import com.syswiki.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl
    extends ServiceImpl<SysEncySpaceMapper, SysEncySpace>
    implements SpaceService {

    private final SysSystemMemberMapper memberMapper;
    private final SysUserMapper userMapper;

    public SpaceServiceImpl(SysSystemMemberMapper memberMapper, SysUserMapper userMapper) {
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<SpaceVO> listActiveSpaces() {
        LambdaQueryWrapper<SysEncySpace> w = new LambdaQueryWrapper<>();
        w.eq(SysEncySpace::getStatus, "ACTIVE").orderByDesc(SysEncySpace::getUpdateTime);
        return list(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SpaceVO getSpaceDetail(String systemId) {
        SysEncySpace entity = getById(systemId);
        if (entity == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
        return toVO(entity);
    }

    @Override
    public SpaceVO createSpace(SpaceCreateDTO dto) {
        LambdaQueryWrapper<SysEncySpace> w = new LambdaQueryWrapper<>();
        w.eq(SysEncySpace::getSystemCode, dto.getSystemCode());
        if (count(w) > 0) throw new BizException(ErrorCode.SPACE_CODE_DUPLICATE);

        SysEncySpace entity = new SysEncySpace();
        entity.setSystemId(IdGenerator.nextId("SP"));
        entity.setSystemName(dto.getSystemName());
        entity.setSystemCode(dto.getSystemCode());
        entity.setOwner(dto.getOwner());
        entity.setDescription(dto.getDescription());
        entity.setStatus("ACTIVE");
        entity.setCreateTime(LocalDateTime.now());
        save(entity);
        return toVO(entity);
    }

    @Override
    public SpaceVO updateSpace(String systemId, SpaceCreateDTO dto) {
        SysEncySpace entity = getById(systemId);
        if (entity == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
        if (dto.getSystemName() != null) entity.setSystemName(dto.getSystemName());
        if (dto.getOwner() != null) entity.setOwner(dto.getOwner());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        updateById(entity);
        return toVO(entity);
    }

    @Override
    public void validateSpaceExists(String systemId) {
        if (getById(systemId) == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
    }

    private SpaceVO toVO(SysEncySpace e) {
        SpaceVO vo = new SpaceVO();
        vo.setSystemId(e.getSystemId());
        vo.setSystemName(e.getSystemName());
        vo.setSystemCode(e.getSystemCode());
        vo.setDescription(e.getDescription());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());

        // 从 sys_system_member 查真正的 OWNER，显示其昵称
        String ownerDisplay = resolveOwnerNickname(e.getSystemId());
        vo.setOwner(ownerDisplay != null ? ownerDisplay : e.getOwner());

        return vo;
    }

    /**
     * 查询系统的负责人显示名（多个EDITOR用顿号连接）
     * 优先级：EDITOR成员 > ADMIN
     */
    private String resolveOwnerNickname(String systemId) {
        LambdaQueryWrapper<SysSystemMember> w = new LambdaQueryWrapper<>();
        w.eq(SysSystemMember::getSystemId, systemId);
        List<SysSystemMember> members = memberMapper.selectList(w);
        if (members == null || members.isEmpty()) return null;

        // 收集所有 EDITOR 身份的成员昵称
        java.util.List<String> editorNames = new java.util.ArrayList<>();
        String adminName = null;

        for (SysSystemMember m : members) {
            SysUser user = userMapper.selectById(m.getUserId());
            if (user == null) continue;
            String name = user.getNickname() != null ? user.getNickname() : user.getUsername();
            if ("ADMIN".equals(user.getRole())) {
                adminName = name;
            } else {
                editorNames.add(name);
            }
        }

        if (!editorNames.isEmpty()) {
            return String.join("、", editorNames);
        }
        return adminName;
    }
}
