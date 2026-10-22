package com.ticketing.common.api;

public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    public BusinessException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
    public static BusinessException notFound() { return new BusinessException(404, "NOT_FOUND", "记录不存在或无权访问"); }
    public static BusinessException conflict(String message) { return new BusinessException(409, "CONFLICT", message); }
    public static BusinessException invalid(String message) { return new BusinessException(400, "INVALID_INPUT", message); }
}
