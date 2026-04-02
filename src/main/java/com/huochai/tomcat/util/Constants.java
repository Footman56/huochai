package com.huochai.tomcat.util;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP常量定义
 * 
 * @Description 定义HTTP协议相关的常量，包括状态码、状态消息、默认配置等
 */
public class Constants {

    // ==================== 服务器配置 ====================
    
    /** 默认端口 */
    public static final int DEFAULT_PORT = 8080;
    
    /** 默认主机 */
    public static final String DEFAULT_HOST = "localhost";
    
    /** 默认工作目录 */
    public static final String DEFAULT_WORK_DIR = "/tmp/tomcat";
    
    /** 请求超时时间(毫秒) */
    public static final int DEFAULT_TIMEOUT = 30000;
    
    /** 最大连接数 */
    public static final int MAX_CONNECTIONS = 200;

    // ==================== HTTP状态码 ====================
    
    /** 200 OK */
    public static final int SC_OK = 200;
    
    /** 201 Created */
    public static final int SC_CREATED = 201;
    
    /** 204 No Content */
    public static final int SC_NO_CONTENT = 204;
    
    /** 301 Moved Permanently */
    public static final int SC_MOVED_PERMANENTLY = 301;
    
    /** 302 Found */
    public static final int SC_FOUND = 302;
    
    /** 304 Not Modified */
    public static final int SC_NOT_MODIFIED = 304;
    
    /** 400 Bad Request */
    public static final int SC_BAD_REQUEST = 400;
    
    /** 401 Unauthorized */
    public static final int SC_UNAUTHORIZED = 401;
    
    /** 403 Forbidden */
    public static final int SC_FORBIDDEN = 403;
    
    /** 404 Not Found */
    public static final int SC_NOT_FOUND = 404;
    
    /** 405 Method Not Allowed */
    public static final int SC_METHOD_NOT_ALLOWED = 405;
    
    /** 500 Internal Server Error */
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    
    /** 502 Bad Gateway */
    public static final int SC_BAD_GATEWAY = 502;
    
    /** 503 Service Unavailable */
    public static final int SC_SERVICE_UNAVAILABLE = 503;

    // ==================== HTTP状态消息 ====================
    
    /** 状态码对应的默认消息 */
    public static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();
    
    static {
        STATUS_MESSAGES.put(SC_OK, "OK");
        STATUS_MESSAGES.put(SC_CREATED, "Created");
        STATUS_MESSAGES.put(SC_NO_CONTENT, "No Content");
        STATUS_MESSAGES.put(SC_MOVED_PERMANENTLY, "Moved Permanently");
        STATUS_MESSAGES.put(SC_FOUND, "Found");
        STATUS_MESSAGES.put(SC_NOT_MODIFIED, "Not Modified");
        STATUS_MESSAGES.put(SC_BAD_REQUEST, "Bad Request");
        STATUS_MESSAGES.put(SC_UNAUTHORIZED, "Unauthorized");
        STATUS_MESSAGES.put(SC_FORBIDDEN, "Forbidden");
        STATUS_MESSAGES.put(SC_NOT_FOUND, "Not Found");
        STATUS_MESSAGES.put(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
        STATUS_MESSAGES.put(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        STATUS_MESSAGES.put(SC_BAD_GATEWAY, "Bad Gateway");
        STATUS_MESSAGES.put(SC_SERVICE_UNAVAILABLE, "Service Unavailable");
    }

    // ==================== HTTP方法 ====================
    
    /** GET方法 */
    public static final String METHOD_GET = "GET";
    
    /** POST方法 */
    public static final String METHOD_POST = "POST";
    
    /** PUT方法 */
    public static final String METHOD_PUT = "PUT";
    
    /** DELETE方法 */
    public static final String METHOD_DELETE = "DELETE";
    
    /** HEAD方法 */
    public static final String METHOD_HEAD = "HEAD";
    
    /** OPTIONS方法 */
    public static final String METHOD_OPTIONS = "OPTIONS";
    
    /** TRACE方法 */
    public static final String METHOD_TRACE = "TRACE";
    
    /** CONNECT方法 */
    public static final String METHOD_CONNECT = "CONNECT";

    // ==================== 常用HTTP头 ====================
    
    /** Content-Type */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    
    /** Content-Length */
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    
    /** Content-Encoding */
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    
    /** Server */
    public static final String HEADER_SERVER = "Server";
    
    /** Date */
    public static final String HEADER_DATE = "Date";
    
    /** Location */
    public static final String HEADER_LOCATION = "Location";
    
    /** Cache-Control */
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    
    /** Cookie */
    public static final String HEADER_COOKIE = "Cookie";
    
    /** Set-Cookie */
    public static final String HEADER_SET_COOKIE = "Set-Cookie";

    // ==================== Content-Type ====================
    
    /** 纯文本 */
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    
    /** HTML */
    public static final String CONTENT_TYPE_HTML = "text/html";
    
    /** JSON */
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    /** XML */
    public static final String CONTENT_TYPE_XML = "application/xml";
    
    /** URL编码表单 */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    
    /** Multipart表单 */
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    
    /** 字节流 */
    public static final String CONTENT_TYPE_OCTET = "application/octet-stream";
    
    /** 默认Content-Type */
    public static final String DEFAULT_CONTENT_TYPE = CONTENT_TYPE_HTML + ";charset=UTF-8";

    // ==================== 字符编码 ====================
    
    /** 默认字符编码 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /** ISO编码 */
    public static final String CHARSET_ISO = "ISO-8859-1";
    
    /** GBK编码 */
    public static final String CHARSET_GBK = "GBK";

    // ==================== 协议版本 ====================
    
    /** 默认HTTP版本 */
    public static final String DEFAULT_PROTOCOL = "HTTP/1.1";
    
    /** 服务器名称 */
    public static final String SERVER_NAME = "MiniTomcat/1.0";

    // ==================== 私有构造方法 ====================
    
    private Constants() {
        // 工具类不允许实例化
    }
}
