package com.syswiki.model.dto;

import java.util.Map;

public class SqlRenderRequest {
    private Map<String, String> params;

    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
}
