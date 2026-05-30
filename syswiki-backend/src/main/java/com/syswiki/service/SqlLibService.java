package com.syswiki.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.syswiki.model.entity.SysEncySqlLib;
import com.syswiki.model.dto.SqlLibSaveDTO;
import com.syswiki.model.vo.SqlLibVO;
import java.util.List;
import java.util.Map;

public interface SqlLibService extends IService<SysEncySqlLib> {
    List<SqlLibVO> listByCategory(String systemId, String category);
    SqlLibVO addSql(String systemId, SqlLibSaveDTO dto);
    String renderSql(String systemId, String sqlId, Map<String, String> params);
    SqlLibVO getSqlDetail(String systemId, String sqlId);
}
