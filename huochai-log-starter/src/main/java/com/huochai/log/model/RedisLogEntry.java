package com.huochai.log.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Redis 操作日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RedisLogEntry extends LogEntry {
    
    /** Redis 命令 */
    private String command;
    
    /** Key */
    private String key;
    
    /** Value */
    private String value;
    
    /** 过期时间 (秒) */
    private Long ttl;
    
    /** 数据库索引 */
    private Integer database;
    
    /** Redis 节点地址 */
    private String node;
}