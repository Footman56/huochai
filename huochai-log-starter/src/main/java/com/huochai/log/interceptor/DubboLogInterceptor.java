package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.DubboLogEntry;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo RPC 日志拦截器
 * 通过 Dubbo Filter SPI 扩展点实现
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER})
public class DubboLogInterceptor implements Filter {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;
    
    public DubboLogInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        LogProperties.DubboConfig config = logProperties.getDubbo();
        if (!config.isEnabled()) {
            return invoker.invoke(invocation);
        }
        
        long startTime = System.currentTimeMillis();
        RpcContext rpcContext = RpcContext.getContext();
        
        DubboLogEntry logEntry = DubboLogEntry.builder()
                .logType(LogType.DUBBO.getCode())
                .level("INFO")
                .serviceInterface(invoker.getInterface().getName())
                .methodName(invocation.getMethodName())
                .side(rpcContext.isProviderSide() ? "provider" : "consumer")
                .remoteAddress(rpcContext.getRemoteAddressString())
                .localAddress(rpcContext.getLocalAddressString())
                .application(rpcContext.getRemoteHostName())
                .group(invoker.getUrl().getParameter(CommonConstants.GROUP_KEY))
                .version(invoker.getUrl().getParameter(CommonConstants.VERSION_KEY))
                .build();
        
        // 记录参数
        if (config.isLogArguments()) {
            try {
                Object[] args = invocation.getArguments();
                if (args != null && args.length > 0) {
                    logEntry.setArguments(objectMapper.writeValueAsString(args));
                }
            } catch (Exception e) {
                logEntry.setArguments("[serialization failed]");
            }
        }
        
        try {
            Result result = invoker.invoke(invocation);
            long duration = System.currentTimeMillis() - startTime;
            
            logEntry.setDuration(duration);
            logEntry.setStatus("SUCCESS");
            
            // 记录返回值
            if (config.isLogResult() && result.getValue() != null) {
                try {
                    logEntry.setResult(objectMapper.writeValueAsString(result.getValue()));
                } catch (Exception e) {
                    logEntry.setResult("[serialization failed]");
                }
            }
            
            logCollector.collect(logEntry);
            return result;
        } catch (RpcException e) {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.setDuration(duration);
            logEntry.setStatus("FAILURE");
            logEntry.setLevel("ERROR");
            logEntry.setErrorMessage(e.getMessage());
            logCollector.collect(logEntry);
            throw e;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.setDuration(duration);
            logEntry.setStatus("FAILURE");
            logEntry.setLevel("ERROR");
            logEntry.setErrorMessage(e.getMessage());
            logCollector.collect(logEntry);
            throw new RpcException(e);
        }
    }
}