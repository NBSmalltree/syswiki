package com.syswiki.controller;

import com.syswiki.auth.PermissionService;
import com.syswiki.model.dto.ContentSaveDTO;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.model.vo.ContentVersionVO;
import com.syswiki.model.vo.Result;
import com.syswiki.service.ContentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/spaces/{systemId}/contents")
public class ContentController {
    private final ContentService contentService;
    private final PermissionService permissionService;

    public ContentController(ContentService contentService, PermissionService permissionService) {
        this.contentService = contentService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Result<List<ContentVO>> list(@PathVariable String systemId) { return Result.success(contentService.listContents(systemId)); }

    @GetMapping("/{moduleType}")
    public Result<ContentVO> get(@PathVariable String systemId, @PathVariable String moduleType) { return Result.success(contentService.getContent(systemId, moduleType)); }

    @PutMapping("/{moduleType}")
    public Result<ContentVO> save(@PathVariable String systemId, @PathVariable String moduleType,
                                  @RequestBody @Valid ContentSaveDTO dto, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        // 自动填充operator
        dto.setOperator((String) request.getAttribute("currentUsername"));
        return Result.success(contentService.saveContent(systemId, moduleType, dto));
    }

    @PostMapping("/import")
    public Result<Void> importMd(@PathVariable String systemId, @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String userId = (String) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentRole");
        permissionService.requireEditPermission(userId, role, systemId);
        contentService.importMarkdown(systemId, file, (String) request.getAttribute("currentUsername"));
        return Result.success(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@PathVariable String systemId, @RequestParam(required = false) List<String> modules) {
        String md = contentService.exportMarkdown(systemId, modules);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=syswiki-export.md")
            .contentType(MediaType.TEXT_PLAIN).body(md.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{moduleType}/versions")
    public Result<List<ContentVersionVO>> versions(@PathVariable String systemId, @PathVariable String moduleType) { return Result.success(contentService.getVersionHistory(systemId, moduleType)); }
}
