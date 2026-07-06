package com.syswiki.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.mapper.SysUserMapper;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.ContentVO;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.service.ContentService;
import com.syswiki.service.SpaceService;
import com.syswiki.service.VectorSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final SysUserMapper userMapper;
    private final SpaceService spaceService;
    private final ContentService contentService;
    private final VectorSyncService vectorSyncService;

    public DataInitializer(SysUserMapper userMapper, SpaceService spaceService,
                           ContentService contentService, VectorSyncService vectorSyncService) {
        this.userMapper = userMapper;
        this.spaceService = spaceService;
        this.contentService = contentService;
        this.vectorSyncService = vectorSyncService;
    }

    @Override
    public void run(String... args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 确保admin账号存在且密码正确
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getUsername, "admin");
        SysUser admin = userMapper.selectOne(w);

        if (admin == null) {
            // admin不存在，创建
            SysUser newAdmin = new SysUser();
            newAdmin.setUserId("U00000000000001");
            newAdmin.setUsername("admin");
            newAdmin.setPassword(encoder.encode("123456"));
            newAdmin.setNickname("系统管理员");
            newAdmin.setRole("ADMIN");
            newAdmin.setStatus("ACTIVE");
            userMapper.insert(newAdmin);
            log.info("已创建管理员账号 admin/123456");
        } else if (!encoder.matches("123456", admin.getPassword())) {
            // admin存在但密码不对，重置
            admin.setPassword(encoder.encode("123456"));
            userMapper.updateById(admin);
            log.info("已重置管理员密码为 123456");
        } else {
            log.info("管理员账号正常");
        }

        // 启动时重建向量库（异步执行，不阻塞启动）
        rebuildVectorStores();
    }

    /**
     * 从数据库全量重建向量存储
     * 应用重启后 InProcessEmbeddingStore 为空，需要从数据库恢复
     */
    private void rebuildVectorStores() {
        try {
            List<SpaceVO> activeSpaces = spaceService.listActiveSpaces();
            if (activeSpaces.isEmpty()) {
                log.info("无活跃系统空间，跳过向量重建");
                return;
            }

            for (SpaceVO space : activeSpaces) {
                String systemId = space.getSystemId();
                List<ContentVO> contents = contentService.listContents(systemId);

                if (contents.isEmpty()) {
                    log.info("系统空间无内容，跳过向量重建: systemId={}", systemId);
                    continue;
                }

                List<String> allContents = contents.stream()
                    .map(ContentVO::getMdContent)
                    .collect(Collectors.toList());
                List<String> moduleTypes = contents.stream()
                    .map(ContentVO::getModuleType)
                    .collect(Collectors.toList());

                log.info("提交向量重建任务: systemId={}, modules={}", systemId, moduleTypes.size());
                vectorSyncService.rebuildAll(systemId, allContents, moduleTypes);
            }

            log.info("已提交 {} 个系统空间的向量重建任务，后台异步执行中...", activeSpaces.size());
        } catch (Exception e) {
            log.error("向量重建初始化异常（向量库仍可在运行时通过内容更新重建）", e);
        }
    }
}
