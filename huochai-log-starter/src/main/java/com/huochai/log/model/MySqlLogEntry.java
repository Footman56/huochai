package com.huochai.log.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * MySQL 数据库操作日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MySqlLogEntry extends LogEntry {
    
    /** SQL 语句 */
    private String sql;
    
    /** SQL 类型 (SELECT/INSERT/UPDATE/DELETE) */
    private String sqlType;
    
    /** 数据源名称 */
    private String datasource;
    
    /** 目标表 */
    private String table;
    
    /** SQL 参数 */
    private String parameters;
    
    /** 影响行数 */
    private Integer affectedRows;
    
    /** Mapper 类名 */
    private String mapperClass;
    
    /** Mapper 方法 */
    private String mapperMethod;
}