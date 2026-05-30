package com.syswiki.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_ency_topology")
public class SysEncyTopology {
    @TableId
    private String linkId;
    private String systemId;
    private String fromNode;
    private String toNode;
    private String protocol;
    private String interfaceName;
    private String interfaceDetails;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }
    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
    public String getFromNode() { return fromNode; }
    public void setFromNode(String fromNode) { this.fromNode = fromNode; }
    public String getToNode() { return toNode; }
    public void setToNode(String toNode) { this.toNode = toNode; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }
    public String getInterfaceDetails() { return interfaceDetails; }
    public void setInterfaceDetails(String interfaceDetails) { this.interfaceDetails = interfaceDetails; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
