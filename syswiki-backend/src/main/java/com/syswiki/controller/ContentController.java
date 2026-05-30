package com.syswiki.controller;

import com.syswiki.model.dto.ContentSaveDTO;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.model.vo.ContentVersionVO;
import com.syswiki.model.vo.Result;
import com.syswiki.service.ContentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/spaces/{systemId}/contents")
public class ContentController {
    private final ContentService contentService;
    public ContentController(ContentService contentService) { this.contentService = contentService; }

    @GetMapping
    public Result<List<ContentVO>> list(@PathVariable String systemId) { return Result.success(contentService.listContents(systemId)); }

    @GetMapping("/{moduleType}")
    public Result<ContentVO> get(@PathVariable String systemId, @PathVariable String moduleType) { return Result.success(contentService.getContent(systemId, moduleType)); }

    @PutMapping("/{moduleType}")
    public Result<ContentVO> save(@PathVariable String systemId, @PathVariable String moduleType, @RequestBody @Valid ContentSaveDTO dto) { return Result.success(contentService.saveContent(systemId, moduleType, dto)); }

    @PostMapping("/import")
    public Result<Void> importMd(@PathVariable String systemId, @RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "系统用户") String operator) {
        contentService.importMarkdown(systemId, file, operator);
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
