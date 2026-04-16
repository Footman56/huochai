package com.huochai.log.model;

import com.huochai.log.context.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Controller 层日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ControllerLogEntry extends LogEntry {

    /**
     * HTTP 方法
     */
    private String httpMethod;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求参数
     */
    private String queryParams;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态码
     */
    private Integer responseStatus;

    /**
     * 响应体
     */
    private String responseBody;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 请求头
     */
    private String requestHeaders;
}