package com.syswiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.vo.SpaceVO;
import java.util.List;

public interface SpaceService extends IService<SysEncySpace> {
    List<SpaceVO> listActiveSpaces();
    SpaceVO getSpaceDetail(String systemId);
    SpaceVO createSpace(SpaceCreateDTO dto);
    SpaceVO updateSpace(String systemId, SpaceCreateDTO dto);
    void validateSpaceExists(String systemId);
}
