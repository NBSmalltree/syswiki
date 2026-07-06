package com.syswiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syswiki.model.entity.SysEncySpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysEncySpaceMapper extends BaseMapper<SysEncySpace> {

    /**
     * 查询用户通过 sys_system_member 关联的所有活跃系统（单次 JOIN 查询替代 N+1）
     */
    @Select("SELECT DISTINCT s.* FROM sys_ency_space s " +
            "INNER JOIN sys_system_member m ON s.system_id = m.system_id " +
            "WHERE m.user_id = #{userId} AND s.status = 'ACTIVE' " +
            "ORDER BY s.update_time DESC")
    List<SysEncySpace> selectSpacesByUserId(@Param("userId") String userId);
}
