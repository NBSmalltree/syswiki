package com.syswiki.util;

import com.syswiki.model.entity.SysEncySpace;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.SpaceVO;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.model.vo.UserVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一的对象转换工具类，集中管理 Entity -> VO 的映射逻辑。
 * <p>
 * 所有方法均为静态无状态，不依赖 Spring 容器。
 */
public final class BeanConverter {

    private BeanConverter() {
        // 工具类禁止实例化
    }

    // ======================== User ========================

    /**
     * SysUser -> UserVO（使用实体自身的 role）
     */
    public static UserVO toUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * SysUser -> UserVO，允许覆盖 role（用于系统成员场景，取系统级角色）
     *
     * @param user      用户实体
     * @param overrideRole 覆盖后的角色，为 null 时不覆盖
     */
    public static UserVO toUserVO(SysUser user, String overrideRole) {
        UserVO vo = toUserVO(user);
        if (vo != null && overrideRole != null) {
            vo.setRole(overrideRole);
        }
        return vo;
    }

    /**
     * 批量 SysUser -> UserVO
     */
    public static List<UserVO> toUserVOList(List<SysUser> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(BeanConverter::toUserVO)
                .collect(Collectors.toList());
    }

    // ======================== Token ========================

    /**
     * SysUser -> TokenVO
     */
    public static TokenVO toTokenVO(SysUser user, String token) {
        if (user == null) {
            return null;
        }
        TokenVO vo = new TokenVO();
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    // ======================== Space ========================

    /**
     * SysEncySpace -> SpaceVO（基础字段映射，不含 owner 显示名解析）
     */
    public static SpaceVO toSpaceVO(SysEncySpace entity) {
        if (entity == null) {
            return null;
        }
        SpaceVO vo = new SpaceVO();
        vo.setSystemId(entity.getSystemId());
        vo.setSystemName(entity.getSystemName());
        vo.setSystemCode(entity.getSystemCode());
        vo.setOwner(entity.getOwner());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
