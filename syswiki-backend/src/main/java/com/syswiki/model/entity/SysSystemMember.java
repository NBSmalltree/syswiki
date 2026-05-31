package com.syswiki.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_system_member")
public class SysSystemMember {
    @TableId
    private String id;
    private String systemId;
    private String userId;
    private String role;
    private LocalDateTime createTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
