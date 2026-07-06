package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysEncyTopologyMapper;
import com.syswiki.model.dto.TopologySaveDTO;
import com.syswiki.model.entity.SysEncyTopology;
import com.syswiki.model.vo.TopologyVO;
import com.syswiki.service.SpaceService;
import com.syswiki.service.TopologyService;
import com.syswiki.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopologyServiceImpl extends ServiceImpl<SysEncyTopologyMapper, SysEncyTopology> implements TopologyService {
    private final SpaceService spaceService;
    public TopologyServiceImpl(SpaceService spaceService) { this.spaceService = spaceService; }

    @Override
    public List<TopologyVO> listTopologies(String systemId) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyTopology> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyTopology::getSystemId, systemId).orderByAsc(SysEncyTopology::getSortOrder);
        return list(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TopologyVO> batchSave(String systemId, List<TopologySaveDTO> links) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyTopology> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyTopology::getSystemId, systemId);
        remove(w);
        List<SysEncyTopology> entities = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            TopologySaveDTO dto = links.get(i);
            SysEncyTopology e = new SysEncyTopology();
            e.setLinkId(IdGenerator.nextId("TL"));
            e.setSystemId(systemId);
            e.setFromNode(dto.getFromNode());
            e.setToNode(dto.getToNode());
            e.setProtocol(dto.getProtocol());
            e.setInterfaceName(dto.getInterfaceName());
            e.setInterfaceDetails(dto.getInterfaceDetails());
            e.setSortOrder(i);
            e.setCreateTime(LocalDateTime.now());
            e.setUpdateTime(LocalDateTime.now());
            entities.add(e);
        }
        saveBatch(entities);
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void deleteTopology(String systemId, String linkId) {
        SysEncyTopology e = getById(linkId);
        if (e == null || !e.getSystemId().equals(systemId)) throw new BizException(ErrorCode.NOT_FOUND, "拓扑链路不存在");
        removeById(linkId);
    }

    private TopologyVO toVO(SysEncyTopology e) {
        TopologyVO vo = new TopologyVO();
        vo.setLinkId(e.getLinkId());
        vo.setSystemId(e.getSystemId());
        vo.setFromNode(e.getFromNode());
        vo.setToNode(e.getToNode());
        vo.setProtocol(e.getProtocol());
        vo.setInterfaceName(e.getInterfaceName());
        vo.setInterfaceDetails(e.getInterfaceDetails());
        vo.setSortOrder(e.getSortOrder());
        return vo;
    }
}
