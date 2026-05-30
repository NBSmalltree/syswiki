package com.syswiki.service;

import com.syswiki.model.entity.SysEncyTopology;
import com.syswiki.model.dto.TopologySaveDTO;
import com.syswiki.model.vo.TopologyVO;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface TopologyService extends IService<SysEncyTopology> {
    List<TopologyVO> listTopologies(String systemId);
    List<TopologyVO> batchSave(String systemId, List<TopologySaveDTO> links);
    void deleteTopology(String systemId, String linkId);
}
