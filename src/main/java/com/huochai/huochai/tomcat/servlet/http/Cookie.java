package com.huochai.huochai.tomcat.servlet.http;

/**
 * HTTP Cookie
 * 
 * @Description 表示HTTP Cookie，包含名称、值和属性
 */
public class Cookie {
    
    /** Cookie名称 */
    private String name;
    
    /** Cookie值 */
    private String value;
    
    /** 属性 */
    private String comment;
    private String domain;
    private int maxAge = -1;
    private String path;
    private boolean secure = false;
    private int version = 0;
    private boolean httpOnly = false;
    
    /**
     * 构造方法
     * 
     * @param name Cookie名称
     * @param value Cookie值
     */
    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * 获取名称
     * @return 名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取值
     * @return 值
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 设置值
     * @param value 值
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * 获取注释
     * @return 注释
     */
    public String getComment() {
        return comment;
    }
    
    /**
     * 设置注释
     * @param comment 注释
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * 获取域
     * @return 域
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * 设置域
     * @param domain 域
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * 获取最大年龄（秒）
     * @return 最大年龄
     */
    public int getMaxAge() {
        return maxAge;
    }
    
    /**
     * 设置最大年龄（秒）
     * @param maxAge 最大年龄
     */
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
    
    /**
     * 获取路径
     * @return 路径
     */
    public String getPath() {
        return path;
    }
    
    /**
     * 设置路径
     * @param path 路径
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * 是否安全
     * @return 是否安全
     */
    public boolean getSecure() {
        return secure;
    }
    
    /**
     * 设置安全标志
     * @param secure 安全标志
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    /**
     * 获取版本
     * @return 版本
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * 设置版本
     * @param version 版本
     */
    public void setVersion(int version) {
        this.version = version;
    }
    
    /**
     * 是否HttpOnly
     * @return 是否HttpOnly
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }
    
    /**
     * 设置HttpOnly标志
     * @param httpOnly HttpOnly标志
     */
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }
    
    /**
     * 生成Cookie头字符串
     * @return Cookie头字符串
     */
    public String toCookieString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        
        if (path != null) {
            sb.append("; Path=").append(path);
        }
        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }
        if (maxAge >= 0) {
            sb.append("; Max-Age=").append(maxAge);
        }
        if (secure) {
            sb.append("; Secure");
        }
        if (httpOnly) {
            sb.append("; HttpOnly");
        }
        
        return sb.toString();
    }
}
