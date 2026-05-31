package com.syswiki.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syswiki.model.entity.SysUser;
import com.syswiki.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final SysUserMapper userMapper;

    public DataInitializer(SysUserMapper userMapper) { this.userMapper = userMapper; }

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
    }
}
