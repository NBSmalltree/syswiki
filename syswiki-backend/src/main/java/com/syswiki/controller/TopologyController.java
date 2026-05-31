package com.syswiki.controller;

import com.syswiki.auth.PermissionService;
import com.syswiki.model.dto.TopologySaveDTO;
import com.syswiki.model.vo.Result;
import com.syswiki.model.vo.TopologyVO;
import com.syswiki.service.TopologyService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/spaces/{systemId}/topologies")
public class TopologyController {
    private final TopologyService topologyService;
    private final PermissionService permissionService;

    public TopologyController(TopologyService topologyService, PermissionService permissionService) {
        this.topologyService = topologyService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Result<List<TopologyVO>> list(@PathVariable String systemId) { return Result.success(topologyService.listTopologies(systemId)); }

    @PostMapping
    public Result<List<TopologyVO>> batchSave(@PathVariable String systemId, @RequestBody @Valid List<TopologySaveDTO> links, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        return Result.success(topologyService.batchSave(systemId, links));
    }

    @DeleteMapping("/{linkId}")
    public Result<Void> delete(@PathVariable String systemId, @PathVariable String linkId, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        topologyService.deleteTopology(systemId, linkId);
        return Result.success(null);
    }
}
