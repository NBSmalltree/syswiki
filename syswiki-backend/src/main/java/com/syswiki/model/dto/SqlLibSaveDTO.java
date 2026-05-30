package com.syswiki.model.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class SqlLibSaveDTO {
    @NotBlank private String title;
    @NotBlank private String category;
    @NotBlank private String sqlTemplate;
    private String description;
    private List<Map<String, String>> params;
    private Integer sortOrder;
    private String operator;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSqlTemplate() { return sqlTemplate; }
    public void setSqlTemplate(String sqlTemplate) { this.sqlTemplate = sqlTemplate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Map<String, String>> getParams() { return params; }
    public void setParams(List<Map<String, String>> params) { this.params = params; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
