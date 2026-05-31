package com.syswiki.model.vo;

import java.time.LocalDateTime;

public class ContentVersionVO {
    private String versionId;
    private Integer version;
    private String operator;
    private String mdContent;
    private LocalDateTime createTime;

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getMdContent() { return mdContent; }
    public void setMdContent(String mdContent) { this.mdContent = mdContent; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
