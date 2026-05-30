package com.syswiki.controller;

import com.syswiki.model.dto.SpaceCreateDTO;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.SpaceService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {
    private final SpaceService spaceService;
    public SpaceController(SpaceService spaceService) { this.spaceService = spaceService; }

    @GetMapping
    public Result<List<SpaceVO>> list() { return Result.success(spaceService.listActiveSpaces()); }

    @GetMapping("/{systemId}")
    public Result<SpaceVO> detail(@PathVariable String systemId) { return Result.success(spaceService.getSpaceDetail(systemId)); }

    @PostMapping
    public Result<SpaceVO> create(@RequestBody @Valid SpaceCreateDTO dto) { return Result.success(spaceService.createSpace(dto)); }

    @PutMapping("/{systemId}")
    public Result<SpaceVO> update(@PathVariable String systemId, @RequestBody @Valid SpaceCreateDTO dto) { return Result.success(spaceService.updateSpace(systemId, dto)); }
}
