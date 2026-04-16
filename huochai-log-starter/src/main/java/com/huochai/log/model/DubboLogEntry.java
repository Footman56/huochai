package com.huochai.log.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Dubbo RPC 日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DubboLogEntry extends LogEntry {
    
    /** 服务接口名 */
    private String serviceInterface;
    
    /** 方法名 */
    private String methodName;
    
    /** 调用类型 (consumer/provider) */
    private String side;
    
    /** 远程地址 */
    private String remoteAddress;
    
    /** 本地地址 */
    private String localAddress;
    
    /** 请求参数 */
    private String arguments;
    
    /** 响应结果 */
    private String result;
    
    /** 应用名称 */
    private String application;
    
    /** 分组 */
    private String group;
    
    /** 版本 */
    private String version;
}