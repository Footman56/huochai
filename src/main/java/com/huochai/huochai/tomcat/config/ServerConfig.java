package com.huochai.huochai.tomcat.config;

/**
 * Tomcat服务器配置
 * 
 * @Description 服务器配置类
 */
public class ServerConfig {
    
    // ==================== 服务器配置 ====================
    
    private String serverName = "MiniTomcat";
    private int port = 8080;
    private String hostName = "localhost";
    private String appBase = ".";
    private int maxThreads = 200;
    private int minSpareThreads = 10;
    private int connectionTimeout = 30000;
    
    // ==================== 路径配置 ====================
    
    private String webappsPath = "./webapps";
    private String workPath = "./work";
    private String confPath = "./conf";
    
    // ==================== 构建器 ====================
    
    public static class Builder {
        private final ServerConfig config = new ServerConfig();
        
        public Builder port(int port) {
            config.port = port;
            return this;
        }
        
        public Builder hostName(String hostName) {
            config.hostName = hostName;
            return this;
        }
        
        public Builder serverName(String serverName) {
            config.serverName = serverName;
            return this;
        }
        
        public Builder appBase(String appBase) {
            config.appBase = appBase;
            return this;
        }
        
        public Builder maxThreads(int maxThreads) {
            config.maxThreads = maxThreads;
            return this;
        }
        
        public Builder minSpareThreads(int minSpareThreads) {
            config.minSpareThreads = minSpareThreads;
            return this;
        }
        
        public Builder webappsPath(String path) {
            config.webappsPath = path;
            return this;
        }
        
        public Builder workPath(String path) {
            config.workPath = path;
            return this;
        }
        
        public ServerConfig build() {
            return config;
        }
    }
    
    // ==================== Getter/Setter ====================
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public String getAppBase() {
        return appBase;
    }
    
    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }
    
    public int getMinSpareThreads() {
        return minSpareThreads;
    }
    
    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public String getWebappsPath() {
        return webappsPath;
    }
    
    public void setWebappsPath(String webappsPath) {
        this.webappsPath = webappsPath;
    }
    
    public String getWorkPath() {
        return workPath;
    }
    
    public void setWorkPath(String workPath) {
        this.workPath = workPath;
    }
    
    public String getConfPath() {
        return confPath;
    }
    
    public void setConfPath(String confPath) {
        this.confPath = confPath;
    }
}
