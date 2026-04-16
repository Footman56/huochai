package com.huochai.tomcat.mapper;

import com.huochai.tomcat.core.Wrapper;

/**
 * 映射数据
 * 
 * @Description 封装URL到Servlet的映射信息
 * @DesignPattern 数据传输对象(DTO)
 */
public class Mapping {
    
    /** 映射的Servlet包装器 */
    private Wrapper wrapper;
    
    /** 映射路径 */
    private String urlPattern;
    
    /** 匹配类型 (EXACT, EXTENSION, PATH, DEFAULT) */
    private MappingType mappingType;
    
    /**
     * 映射类型枚举
     */
    public enum MappingType {
        /** 精确匹配 /hello */
        EXACT,
        /** 扩展名匹配 *.do */
        EXTENSION,
        /** 路径匹配 /api/* */
        PATH,
        /** 默认Servlet */
        DEFAULT,
        /** 欢迎文件 */
        WELCOME
    }
    
    /**
     * 构造方法
     * 
     * @param urlPattern URL模式
     * @param wrapper Servlet包装器
     * @param mappingType 映射类型
     */
    public Mapping(String urlPattern, Wrapper wrapper, MappingType mappingType) {
        this.urlPattern = urlPattern;
        this.wrapper = wrapper;
        this.mappingType = mappingType;
    }
    
    /**
     * 获取Servlet包装器
     * @return Wrapper
     */
    public Wrapper getWrapper() {
        return wrapper;
    }
    
    /**
     * 设置Servlet包装器
     * @param wrapper Wrapper
     */
    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }
    
    /**
     * 获取URL模式
     * @return URL模式
     */
    public String getUrlPattern() {
        return urlPattern;
    }
    
    /**
     * 设置URL模式
     * @param urlPattern URL模式
     */
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }
    
    /**
     * 获取映射类型
     * @return 映射类型
     */
    public MappingType getMappingType() {
        return mappingType;
    }
    
    /**
     * 设置映射类型
     * @param mappingType 映射类型
     */
    public void setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
    }
    
    @Override
    public String toString() {
        return "Mapping{" +
                "urlPattern='" + urlPattern + '\'' +
                ", wrapper=" + (wrapper != null ? wrapper.getName() : "null") +
                ", mappingType=" + mappingType +
                '}';
    }
}
