package com.huochai.log.interceptor;

import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.MySqlLogEntry;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.Properties;

/**
 * MySQL 操作日志拦截器
 * 通过 MyBatis 插件机制自动拦截 SQL 执行
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, 
            org.apache.ibatis.session.RowBounds.class, org.apache.ibatis.session.ResultHandler.class})
})
public class MySqlLogInterceptor implements Interceptor {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    
    public MySqlLogInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        LogProperties.DatabaseConfig config = logProperties.getDatabase();
        if (!config.isMysqlEnabled()) {
            return invocation.proceed();
        }
        
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        long startTime = System.currentTimeMillis();
        String sql = null;
        String sqlType = null;
        
        try {
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            sql = boundSql.getSql();
            sqlType = mappedStatement.getSqlCommandType().name();
            
            Object result = invocation.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // 只记录慢查询或超过阈值的SQL
            if (duration >= config.getSlowQueryThreshold() || 
                SqlCommandType.UPDATE.equals(mappedStatement.getSqlCommandType()) ||
                SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType()) ||
                SqlCommandType.DELETE.equals(mappedStatement.getSqlCommandType())) {
                
                MySqlLogEntry logEntry = MySqlLogEntry.builder()
                        .logType(LogType.MYSQL.getCode())
                        .level(duration >= config.getSlowQueryThreshold() ? "WARN" : "INFO")
                        .sql(formatSql(sql))
                        .sqlType(sqlType)
                        .duration(duration)
                        .status("SUCCESS")
                        .mapperClass(mappedStatement.getId().substring(0, mappedStatement.getId().lastIndexOf('.')))
                        .mapperMethod(mappedStatement.getId().substring(mappedStatement.getId().lastIndexOf('.') + 1))
                        .build();
                
                // 设置参数
                if (parameter != null) {
                    logEntry.setParameters(parameter.toString());
                }
                
                // 尝试提取表名
                logEntry.setTable(extractTableName(sql));
                
                logCollector.collect(logEntry);
            }
            
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            MySqlLogEntry logEntry = MySqlLogEntry.builder()
                    .logType(LogType.MYSQL.getCode())
                    .level("ERROR")
                    .sql(formatSql(sql))
                    .sqlType(sqlType)
                    .duration(duration)
                    .status("FAILURE")
                    .errorMessage(e.getMessage())
                    .build();
            
            logCollector.collect(logEntry);
            throw e;
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
    }
    
    private String formatSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.replaceAll("\s+", " ").trim();
    }
    
    private String extractTableName(String sql) {
        if (sql == null) {
            return null;
        }
        String upperSql = sql.toUpperCase();
        try {
            if (upperSql.contains(" FROM ")) {
                int fromIndex = upperSql.indexOf(" FROM ") + 6;
                String afterFrom = sql.substring(fromIndex).trim();
                int spaceIndex = afterFrom.indexOf(' ');
                return spaceIndex > 0 ? afterFrom.substring(0, spaceIndex) : afterFrom;
            } else if (upperSql.contains(" INTO ")) {
                int intoIndex = upperSql.indexOf(" INTO ") + 6;
                String afterInto = sql.substring(intoIndex).trim();
                int spaceIndex = afterInto.indexOf(' ');
                return spaceIndex > 0 ? afterInto.substring(0, spaceIndex) : afterInto;
            } else if (upperSql.contains(" UPDATE ")) {
                int updateIndex = upperSql.indexOf(" UPDATE ") + 8;
                String afterUpdate = sql.substring(updateIndex).trim();
                int spaceIndex = afterUpdate.indexOf(' ');
                return spaceIndex > 0 ? afterUpdate.substring(0, spaceIndex) : afterUpdate;
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
}