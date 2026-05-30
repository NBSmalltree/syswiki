package com.syswiki.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SpaceCreateDTO {
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 128)
    private String systemName;
    @NotBlank(message = "系统代号不能为空")
    @Size(max = 32)
    private String systemCode;
    @NotBlank(message = "负责人不能为空")
    @Size(max = 64)
    private String owner;
    @Size(max = 512)
    private String description;

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
