package com.syswiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import com.syswiki.mapper.SysEncyContentMapper;
import com.syswiki.mapper.SysEncyContentVersionMapper;
import com.syswiki.model.dto.ContentSaveDTO;
import com.syswiki.model.entity.SysEncyContent;
import com.syswiki.model.entity.SysEncyContentVersion;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.model.vo.ContentVersionVO;
import com.syswiki.service.ContentService;
import com.syswiki.service.MarkdownParserService;
import com.syswiki.service.SpaceService;
import com.syswiki.service.VectorSyncService;
import com.syswiki.util.IdGenerator;
import com.syswiki.util.SensitiveWordChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentServiceImpl extends ServiceImpl<SysEncyContentMapper, SysEncyContent> implements ContentService {
    private static final Logger log = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final SpaceService spaceService;
    private final MarkdownParserService markdownParserService;
    private final SysEncyContentVersionMapper versionMapper;
    private final VectorSyncService vectorSyncService;
    private final SensitiveWordChecker sensitiveWordChecker;

    public ContentServiceImpl(SpaceService spaceService, MarkdownParserService markdownParserService,
                              SysEncyContentVersionMapper versionMapper, VectorSyncService vectorSyncService,
                              SensitiveWordChecker sensitiveWordChecker) {
        this.spaceService = spaceService;
        this.markdownParserService = markdownParserService;
        this.versionMapper = versionMapper;
        this.vectorSyncService = vectorSyncService;
        this.sensitiveWordChecker = sensitiveWordChecker;
    }

    @Override
    public List<ContentVO> listContents(String systemId) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyContent> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyContent::getSystemId, systemId);
        return list(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ContentVO getContent(String systemId, String moduleType) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyContent> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyContent::getSystemId, systemId).eq(SysEncyContent::getModuleType, moduleType);
        SysEncyContent entity = getOne(w);
        return entity != null ? toVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentVO saveContent(String systemId, String moduleType, ContentSaveDTO dto) {
        // 敏感词检查
        sensitiveWordChecker.check(dto.getMdContent());

        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyContent> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyContent::getSystemId, systemId).eq(SysEncyContent::getModuleType, moduleType);
        SysEncyContent existing = getOne(w);

        if (existing != null) {
            saveVersionHistory(existing);
            existing.setMdContent(dto.getMdContent());
            existing.setVersion(existing.getVersion() + 1);
            existing.setOperator(dto.getOperator());
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
            // 异步触发向量化
            vectorSyncService.syncContent(systemId, moduleType, dto.getMdContent());
            return toVO(existing);
        } else {
            SysEncyContent entity = new SysEncyContent();
            entity.setContentId(IdGenerator.nextId("CT"));
            entity.setSystemId(systemId);
            entity.setModuleType(moduleType);
            entity.setMdContent(dto.getMdContent());
            entity.setVersion(1);
            entity.setOperator(dto.getOperator());
            entity.setCreateTime(LocalDateTime.now());
            save(entity);
            // 首次创建也记录版本历史
            saveVersionHistory(entity);
            // 异步触发向量化
            vectorSyncService.syncContent(systemId, moduleType, dto.getMdContent());
            return toVO(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importMarkdown(String systemId, MultipartFile file, String operator) {
        spaceService.validateSpaceExists(systemId);
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, String> modules = markdownParserService.parseByHeadings(content);
            for (Map.Entry<String, String> entry : modules.entrySet()) {
                if (entry.getKey() != null) {
                    ContentSaveDTO dto = new ContentSaveDTO();
                    dto.setMdContent(entry.getValue());
                    dto.setOperator(operator);
                    saveContent(systemId, entry.getKey(), dto);
                }
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_IMPORT_ERROR);
        }
    }

    @Override
    public String exportMarkdown(String systemId, List<String> moduleTypes) {
        spaceService.validateSpaceExists(systemId);
        LambdaQueryWrapper<SysEncyContent> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyContent::getSystemId, systemId);
        if (moduleTypes != null && !moduleTypes.isEmpty()) w.in(SysEncyContent::getModuleType, moduleTypes);
        Map<String, String> modules = list(w).stream()
            .collect(Collectors.toMap(SysEncyContent::getModuleType, SysEncyContent::getMdContent, (a, b) -> b));
        return markdownParserService.mergeModules(modules);
    }

    @Override
    public List<ContentVersionVO> getVersionHistory(String systemId, String moduleType) {
        LambdaQueryWrapper<SysEncyContentVersion> w = new LambdaQueryWrapper<>();
        w.eq(SysEncyContentVersion::getSystemId, systemId)
         .eq(SysEncyContentVersion::getModuleType, moduleType)
         .orderByDesc(SysEncyContentVersion::getVersion);
        List<SysEncyContentVersion> versions = versionMapper.selectList(w);

        // 历史为空但内容存在时，自动补建初始历史记录
        if (versions.isEmpty()) {
            LambdaQueryWrapper<SysEncyContent> cw = new LambdaQueryWrapper<>();
            cw.eq(SysEncyContent::getSystemId, systemId).eq(SysEncyContent::getModuleType, moduleType);
            SysEncyContent content = getOne(cw);
            if (content != null) {
                saveVersionHistory(content);
                versions = versionMapper.selectList(w);
            }
        }

        return versions.stream().map(v -> {
            ContentVersionVO vo = new ContentVersionVO();
            vo.setVersionId(v.getVersionId());
            vo.setVersion(v.getVersion());
            vo.setOperator(v.getOperator());
            vo.setMdContent(v.getMdContent());
            vo.setCreateTime(v.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private void saveVersionHistory(SysEncyContent c) {
        SysEncyContentVersion ver = new SysEncyContentVersion();
        ver.setVersionId(IdGenerator.nextId("CV"));
        ver.setContentId(c.getContentId());
        ver.setSystemId(c.getSystemId());
        ver.setModuleType(c.getModuleType());
        ver.setVersion(c.getVersion());
        ver.setMdContent(c.getMdContent());
        ver.setOperator(c.getOperator());
        ver.setCreateTime(LocalDateTime.now());
        versionMapper.insert(ver);
    }

    private ContentVO toVO(SysEncyContent e) {
        ContentVO vo = new ContentVO();
        vo.setContentId(e.getContentId());
        vo.setSystemId(e.getSystemId());
        vo.setModuleType(e.getModuleType());
        vo.setMdContent(e.getMdContent());
        vo.setVersion(e.getVersion());
        vo.setOperator(e.getOperator());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
