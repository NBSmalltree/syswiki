package com.syswiki.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_ency_sql_lib")
public class SysEncySqlLib {
    @TableId
    private String sqlId;
    private String systemId;
    private String title;
    private String category;
    private String sqlTemplate;
    private String description;
    private String paramsJson;
    private Integer sortOrder;
    private String operator;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getSqlId() { return sqlId; }
    public void setSqlId(String sqlId) { this.sqlId = sqlId; }
    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSqlTemplate() { return sqlTemplate; }
    public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
