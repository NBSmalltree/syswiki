package com.syswiki.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_login_log")
public class SysLoginLog {
    @TableId
    private String logId;
    private String username;
    private String loginIp;
    private String status;
    private String message;
    private LocalDateTime createTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLoginIp() { return loginIp; }
    public void setLoginIp(String loginIp) { this.loginIp = loginIp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
