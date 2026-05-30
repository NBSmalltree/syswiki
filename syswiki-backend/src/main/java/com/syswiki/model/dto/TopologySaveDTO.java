package com.syswiki.model.dto;

import javax.validation.constraints.NotBlank;

public class TopologySaveDTO {
    @NotBlank private String fromNode;
    @NotBlank private String toNode;
    private String protocol;
    private String interfaceName;
    private String interfaceDetails;

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
}
