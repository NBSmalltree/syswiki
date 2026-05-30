package com.syswiki.controller;

import com.syswiki.model.dto.SqlRenderRequest;
import com.syswiki.model.dto.SqlLibSaveDTO;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.SqlLibVO;
import com.syswiki.service.SqlLibService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/spaces/{systemId}/sql-lib")
public class SqlLibController {
    private final SqlLibService sqlLibService;
    public SqlLibController(SqlLibService sqlLibService) { this.sqlLibService = sqlLibService; }

    @GetMapping
    public Result<List<SqlLibVO>> list(@PathVariable String systemId, @RequestParam(required = false) String category) { return Result.success(sqlLibService.listByCategory(systemId, category)); }

    @GetMapping("/{sqlId}")
    public Result<SqlLibVO> detail(@PathVariable String systemId, @PathVariable String sqlId) { return Result.success(sqlLibService.getSqlDetail(systemId, sqlId)); }

    @PostMapping
    public Result<SqlLibVO> add(@PathVariable String systemId, @RequestBody @Valid SqlLibSaveDTO dto) { return Result.success(sqlLibService.addSql(systemId, dto)); }

    @PostMapping("/{sqlId}/render")
    public Result<Map<String, String>> render(@PathVariable String systemId, @PathVariable String sqlId, @RequestBody SqlRenderRequest request) {
        String rendered = sqlLibService.renderSql(systemId, sqlId, request.getParams());
        Map<String, String> data = new HashMap<>();
        data.put("renderedSql", rendered);
        return Result.success(data);
    }
}
