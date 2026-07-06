package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.*;
import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.entity.*;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.SpaceService;
import com.syswiki.util.BeanConverter;
import com.syswiki.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl
    extends ServiceImpl<SysEncySpaceMapper, SysEncySpace>
    implements SpaceService {

    private static final Logger log = LoggerFactory.getLogger(SpaceServiceImpl.class);
    private final SysSystemMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final SysEncyContentMapper contentMapper;
    private final SysEncyContentVersionMapper versionMapper;
    private final SysEncyTopologyMapper topologyMapper;
    private final SysEncySqlLibMapper sqlLibMapper;

    public SpaceServiceImpl(SysSystemMemberMapper memberMapper, SysUserMapper userMapper,
                            SysEncyContentMapper contentMapper, SysEncyContentVersionMapper versionMapper,
                            SysEncyTopologyMapper topologyMapper, SysEncySqlLibMapper sqlLibMapper) {
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.versionMapper = versionMapper;
        this.topologyMapper = topologyMapper;
        this.sqlLibMapper = sqlLibMapper;
    }

    @Override
    public List<SpaceVO> listActiveSpaces() {
        LambdaQueryWrapper<SysEncySpace> w = new LambdaQueryWrapper<>();
        w.eq(SysEncySpace::getStatus, "ACTIVE").orderByDesc(SysEncySpace::getUpdateTime);
        return list(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SpaceVO> listUserSpaces(String userId) {
        // 单次 JOIN 查询，替代原来 N+1 次逐个查询权限
        return baseMapper.selectSpacesByUserId(userId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
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
        log.info("系统创建成功: systemId={}, systemName={}, systemCode={}", entity.getSystemId(), entity.getSystemName(), entity.getSystemCode());
        return toVO(entity);
    }

    @Override
    public SpaceVO updateSpace(String systemId, SpaceCreateDTO dto) {
        SysEncySpace entity = getById(systemId);
        if (entity == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
        if (dto.getSystemName() != null && !dto.getSystemName().isEmpty()) entity.setSystemName(dto.getSystemName());
        if (dto.getSystemCode() != null && !dto.getSystemCode().isEmpty()) {
            // 校验代号唯一（排除自身）
            LambdaQueryWrapper<SysEncySpace> w = new LambdaQueryWrapper<>();
            w.eq(SysEncySpace::getSystemCode, dto.getSystemCode()).ne(SysEncySpace::getSystemId, systemId);
            if (count(w) > 0) throw new BizException(ErrorCode.SPACE_CODE_DUPLICATE);
            entity.setSystemCode(dto.getSystemCode());
        }
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        updateById(entity);
        log.info("系统更新成功: systemId={}, systemName={}", systemId, entity.getSystemName());
        return toVO(entity);
    }

    @Override
    public void validateSpaceExists(String systemId) {
        if (getById(systemId) == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpace(String systemId) {
        if (getById(systemId) == null) throw new BizException(ErrorCode.SPACE_NOT_FOUND);
        log.warn("系统删除开始: systemId={}, 将级联删除所有关联数据", systemId);
        // 级联删除所有关联数据
        LambdaQueryWrapper<SysEncyContentVersion> vw = new LambdaQueryWrapper<>();
        vw.eq(SysEncyContentVersion::getSystemId, systemId);
        versionMapper.delete(vw);

        LambdaQueryWrapper<SysEncyContent> cw = new LambdaQueryWrapper<>();
        cw.eq(SysEncyContent::getSystemId, systemId);
        contentMapper.delete(cw);

        LambdaQueryWrapper<SysEncyTopology> tw = new LambdaQueryWrapper<>();
        tw.eq(SysEncyTopology::getSystemId, systemId);
        topologyMapper.delete(tw);

        LambdaQueryWrapper<SysEncySqlLib> sw = new LambdaQueryWrapper<>();
        sw.eq(SysEncySqlLib::getSystemId, systemId);
        sqlLibMapper.delete(sw);

        LambdaQueryWrapper<SysSystemMember> mw = new LambdaQueryWrapper<>();
        mw.eq(SysSystemMember::getSystemId, systemId);
        memberMapper.delete(mw);

        removeById(systemId);
        log.info("系统删除成功: systemId={}", systemId);
    }

    private SpaceVO toVO(SysEncySpace e) {
        SpaceVO vo = BeanConverter.toSpaceVO(e);

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
