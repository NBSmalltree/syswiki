package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysEncySqlLibMapper;
import com.syswiki.model.dto.SqlLibSaveDTO;
import com.syswiki.model.entity.SysEncySqlLib;
import com.syswiki.model.vo.SqlLibVO;
import com.syswiki.service.SqlLibService;
import com.syswiki.service.SpaceService;
import com.syswiki.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SqlLibServiceImpl extends ServiceImpl<SysEncySqlLibMapper, SysEncySqlLib> implements SqlLibService {
    private static final Logger log = LoggerFactory.getLogger(SqlLibServiceImpl.class);
    private final SpaceService spaceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SqlLibServiceImpl(SpaceService spaceService) { this.spaceService = spaceService; }

    @Override
    public List<SqlLibVO> listByCategory(String systemId, String category) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncySqlLib> w = new LambdaQueryWrapper<>();
        w.eq(SysEncySqlLib::getSystemId, systemId);
        if (category != null && !category.isEmpty() && !"ALL".equals(category)) w.eq(SysEncySqlLib::getCategory, category);
        w.orderByAsc(SysEncySqlLib::getSortOrder);
        return list(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SqlLibVO addSql(String systemId, SqlLibSaveDTO dto) {
        spaceService.validateSpaceExists(systemId);
        SysEncySqlLib entity = new SysEncySqlLib();
        entity.setSqlId(IdGenerator.nextId("SQ"));
        entity.setSystemId(systemId);
        entity.setTitle(dto.getTitle());
        entity.setCategory(dto.getCategory());
        entity.setSqlTemplate(dto.getSqlTemplate());
        entity.setDescription(dto.getDescription());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        entity.setOperator(dto.getOperator());
        entity.setCreateTime(LocalDateTime.now());
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            try { entity.setParamsJson(objectMapper.writeValueAsString(dto.getParams())); }
            catch (Exception e) { log.warn("params序列化失败: " + e.getMessage()); }
        }
        save(entity);
        return toVO(entity);
    }

    @Override
    public String renderSql(String systemId, String sqlId, Map<String, String> params) {
        spaceService.validateSpaceExists(systemId);
        SysEncySqlLib entity = getById(sqlId);
        if (entity == null || !entity.getSystemId().equals(systemId)) throw new BizException(ErrorCode.NOT_FOUND, "SQL条目不存在");
        String result = entity.getSqlTemplate();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace(":" + entry.getKey(), "'" + entry.getValue().replace("'", "''") + "'");
        }
        return result;
    }

    @Override
    public SqlLibVO getSqlDetail(String systemId, String sqlId) {
        spaceService.validateSpaceExists(systemId);
        SysEncySqlLib entity = getById(sqlId);
        if (entity == null || !entity.getSystemId().equals(systemId)) throw new BizException(ErrorCode.NOT_FOUND, "SQL条目不存在");
        return toVO(entity);
    }

    private SqlLibVO toVO(SysEncySqlLib e) {
        SqlLibVO vo = new SqlLibVO();
        vo.setSqlId(e.getSqlId());
        vo.setSystemId(e.getSystemId());
        vo.setTitle(e.getTitle());
        vo.setCategory(e.getCategory());
        vo.setSqlTemplate(e.getSqlTemplate());
        vo.setDescription(e.getDescription());
        vo.setSortOrder(e.getSortOrder());
        vo.setOperator(e.getOperator());
        if (e.getParamsJson() != null && !e.getParamsJson().isEmpty()) {
            try { vo.setParams(objectMapper.readValue(e.getParamsJson(), new TypeReference<List<Map<String, String>>>() {})); }
            catch (Exception ex) { vo.setParams(Collections.emptyList()); }
        } else { vo.setParams(Collections.emptyList()); }
        return vo;
    }
}
