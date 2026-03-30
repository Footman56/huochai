package com.huochai.huochai.tomcat.mapper;

import com.huochai.huochai.tomcat.core.Wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet映射器
 * 
 * @Description 根据URL路径查找对应的Servlet包装器
 * @DesignPattern 策略模式 - 支持多种URL匹配策略(精确匹配、扩展名匹配、路径匹配)
 *               同时也应用了简单工厂模式的思想
 */
public class ServletMapper {
    
    /** 精确匹配映射 */
    private Map<String, Mapping> exactMappings = new HashMap<>();
    
    /** 扩展名匹配映射 */
    private Map<String, Mapping> extensionMappings = new HashMap<>();
    
    /** 路径匹配映射（按路径长度降序排列，最长优先匹配） */
    private List<Mapping> pathMappings = new ArrayList<>();
    
    /** 默认Servlet映射 */
    private Mapping defaultMapping;
    
    /**
     * 注册Servlet映射
     * 
     * @Description 将URL模式与Servlet包装器关联
     * @param urlPattern URL模式
     * @param wrapper Servlet包装器
     */
    public void addMapping(String urlPattern, Wrapper wrapper) {
        Mapping.MappingType type = determineMappingType(urlPattern);
        Mapping mapping = new Mapping(urlPattern, wrapper, type);
        
        switch (type) {
            case EXACT:
                exactMappings.put(urlPattern, mapping);
                break;
            case EXTENSION:
                // 去掉*前缀
                extensionMappings.put(urlPattern.substring(1), mapping);
                break;
            case PATH:
                pathMappings.add(mapping);
                // 按路径长度降序排列
                pathMappings.sort((a, b) -> b.getUrlPattern().length() - a.getUrlPattern().length());
                break;
            case DEFAULT:
                defaultMapping = mapping;
                break;
        }
    }
    
    /**
     * 根据URI查找映射
     * 
     * @Description 依次尝试精确匹配、路径匹配、扩展名匹配
     * @DesignPattern 策略模式 - 根据不同的匹配策略查找
     * @param uri 请求URI
     * @return 映射数据，如果未找到返回null
     */
    public Mapping getMapping(String uri) {
        // 1. 尝试精确匹配
        Mapping mapping = exactMappings.get(uri);
        if (mapping != null) {
            return mapping;
        }
        
        // 2. 尝试路径匹配
        for (Mapping pathMapping : pathMappings) {
            String pattern = pathMapping.getUrlPattern();
            // 去掉末尾的/*
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (uri.startsWith(prefix)) {
                return pathMapping;
            }
        }
        
        // 3. 尝试扩展名匹配
        int lastSlash = uri.lastIndexOf('/');
        String extension = lastSlash >= 0 ? uri.substring(lastSlash) : uri;
        if (extension.contains(".")) {
            extension = extension.substring(extension.lastIndexOf('.'));
            mapping = extensionMappings.get(extension);
            if (mapping != null) {
                return mapping;
            }
        }
        
        // 4. 返回默认Servlet
        return defaultMapping;
    }
    
    /**
     * 判断映射类型
     * 
     * @Description 根据URL模式确定匹配类型
     * @param urlPattern URL模式
     * @return 映射类型
     */
    private Mapping.MappingType determineMappingType(String urlPattern) {
        if (urlPattern.equals("/")) {
            return Mapping.MappingType.DEFAULT;
        } else if (urlPattern.startsWith("*.")) {
            return Mapping.MappingType.EXTENSION;
        } else if (urlPattern.endsWith("/*")) {
            return Mapping.MappingType.PATH;
        } else {
            return Mapping.MappingType.EXACT;
        }
    }
    
    /**
     * 移除映射
     * 
     * @Description 移除指定的URL映射
     * @param urlPattern URL模式
     */
    public void removeMapping(String urlPattern) {
        Mapping.MappingType type = determineMappingType(urlPattern);
        
        switch (type) {
            case EXACT:
                exactMappings.remove(urlPattern);
                break;
            case EXTENSION:
                extensionMappings.remove(urlPattern.substring(1));
                break;
            case PATH:
                pathMappings.removeIf(m -> m.getUrlPattern().equals(urlPattern));
                break;
            case DEFAULT:
                defaultMapping = null;
                break;
        }
    }
    
    /**
     * 获取所有映射
     * 
     * @return 所有映射列表
     */
    public List<Mapping> getAllMappings() {
        List<Mapping> all = new ArrayList<>();
        all.addAll(exactMappings.values());
        all.addAll(extensionMappings.values());
        all.addAll(pathMappings);
        if (defaultMapping != null) {
            all.add(defaultMapping);
        }
        return all;
    }
    
    /**
     * 清除所有映射
     */
    public void clear() {
        exactMappings.clear();
        extensionMappings.clear();
        pathMappings.clear();
        defaultMapping = null;
    }
    
    /**
     * 获取映射数量
     * 
     * @return 映射数量
     */
    public int getMappingCount() {
        return exactMappings.size() + extensionMappings.size() + pathMappings.size() 
               + (defaultMapping != null ? 1 : 0);
    }
}
