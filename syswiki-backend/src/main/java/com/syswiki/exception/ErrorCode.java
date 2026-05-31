package com.syswiki.exception;

public enum ErrorCode {
    PARAM_INVALID(400, "参数校验失败"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SPACE_NOT_FOUND(10001, "系统空间不存在"),
    SPACE_CODE_DUPLICATE(10002, "系统代号已存在"),
    MARKDOWN_PARSE_ERROR(10003, "Markdown解析失败"),
    CONTENT_SAVE_ERROR(10004, "内容保存失败"),
    AI_SERVICE_ERROR(10005, "AI服务不可用"),
    TOPOLOGY_DATA_ERROR(10006, "拓扑数据格式错误"),
    SQL_RENDER_ERROR(10007, "SQL参数渲染失败"),
    FILE_IMPORT_ERROR(10008, "文件导入失败"),
    AUTH_FAILED(10009, "认证失败"),
    FORBIDDEN(10010, "无权限");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) { this.code = code; this.message = message; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
}
