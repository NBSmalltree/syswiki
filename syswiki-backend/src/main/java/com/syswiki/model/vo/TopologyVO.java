package com.syswiki.model.vo;

public class TopologyVO {
    private String linkId;
    private String systemId;
    private String fromNode;
    private String toNode;
    private String protocol;
    private String interfaceName;
    private String interfaceDetails;
    private Integer sortOrder;

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
}
