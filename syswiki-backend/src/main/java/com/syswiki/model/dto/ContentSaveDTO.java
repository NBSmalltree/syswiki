package com.syswiki.model.dto;

import javax.validation.constraints.NotBlank;

public class ContentSaveDTO {
    @NotBlank(message = "内容不能为空")
    private String mdContent;
    private String operator;

    public String getMdContent() { return mdContent; }
    public void setMdContent(String mdContent) { this.mdContent = mdContent; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
