package com.huochai.huochai.tomcat.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP工具类
 * 
 * @Description 提供HTTP协议解析和编码解码的工具方法
 */
public class HttpUtil {

    /**
     * 解析查询字符串为Map
     * 
     * @Description 将URL查询参数解析为键值对Map
     * @param queryString 查询字符串 (如: name=Tom&age=18)
     * @return 参数Map
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return params;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = decode(kv[0]);
                String value = decode(kv[1]);
                params.put(key, value);
            } else if (kv.length == 1 && !kv[0].isEmpty()) {
                params.put(decode(kv[0]), "");
            }
        }
        return params;
    }

    /**
     * 将Map转换为查询字符串
     * 
     * @Description 将键值对Map转换为URL查询字符串
     * @param params 参数Map
     * @return 查询字符串
     */
    public static String toQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(encode(entry.getKey()));
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append("=");
                sb.append(encode(entry.getValue()));
            }
            first = false;
        }
        return sb.toString();
    }

    /**
     * URL编码
     * 
     * @Description 对字符串进行URL编码，默认使用UTF-8
     * @param value 待编码字符串
     * @return 编码后的字符串
     */
    public static String encode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLEncoder.encode(value, Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * URL解码
     * 
     * @Description 对字符串进行URL解码，默认使用UTF-8
     * @param value 待解码字符串
     * @return 解码后的字符串
     */
    public static String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * 获取HTTP状态消息
     * 
     * @Description 根据状态码获取默认的状态消息
     * @param statusCode 状态码
     * @return 状态消息
     */
    public static String getStatusMessage(int statusCode) {
        String message = Constants.STATUS_MESSAGES.get(statusCode);
        return message != null ? message : "Unknown Status";
    }

    /**
     * 规范化URI
     * 
     * @Description 去除URI中的多余斜杠，处理相对路径
     * @param uri 原始URI
     * @return 规范化后的URI
     */
    public static String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        
        // 移除查询字符串
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }
        
        // 移除fragment
        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex > 0) {
            uri = uri.substring(0, fragmentIndex);
        }
        
        // 处理多个连续斜杠
        while (uri.contains("//")) {
            uri = uri.replace("//", "/");
        }
        
        // 确保以斜杠开头
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        
        return uri;
    }

    /**
     * 解析请求行
     * 
     * @Description 解析HTTP请求行，提取方法、URI和协议版本
     * @param requestLine 请求行 (如: GET /hello?name=Tom HTTP/1.1)
     * @return 包含method, uri, protocol的Map
     */
    public static Map<String, String> parseRequestLine(String requestLine) {
        Map<String, String> result = new HashMap<>();
        if (requestLine == null || requestLine.isEmpty()) {
            return result;
        }
        
        String[] parts = requestLine.split(" ");
        if (parts.length >= 1) {
            result.put("method", parts[0]);
        }
        if (parts.length >= 2) {
            result.put("uri", parts[1]);
        }
        if (parts.length >= 3) {
            result.put("protocol", parts[2]);
        }
        
        return result;
    }

    /**
     * 生成响应行
     * 
     * @Description 生成HTTP响应状态行
     * @param protocol 协议版本
     * @param statusCode 状态码
     * @return 响应行字符串
     */
    public static String generateResponseLine(String protocol, int statusCode) {
        return protocol + " " + statusCode + " " + getStatusMessage(statusCode);
    }

    /**
     * 判断是否为HTTP方法
     * 
     * @Description 检查给定字符串是否为有效的HTTP方法
     * @param method 方法名
     * @return 是否为有效方法
     */
    public static boolean isValidHttpMethod(String method) {
        if (method == null) {
            return false;
        }
        return Constants.METHOD_GET.equals(method) ||
               Constants.METHOD_POST.equals(method) ||
               Constants.METHOD_PUT.equals(method) ||
               Constants.METHOD_DELETE.equals(method) ||
               Constants.METHOD_HEAD.equals(method) ||
               Constants.METHOD_OPTIONS.equals(method) ||
               Constants.METHOD_TRACE.equals(method) ||
               Constants.METHOD_CONNECT.equals(method);
    }

    /**
     * 获取文件扩展名对应的Content-Type
     * 
     * @Description 根据文件扩展名获取MIME类型
     * @param extension 文件扩展名
     * @return Content-Type
     */
    public static String getContentTypeByExtension(String extension) {
        if (extension == null) {
            return Constants.DEFAULT_CONTENT_TYPE;
        }
        
        extension = extension.toLowerCase();
        switch (extension) {
            case "html":
            case "htm":
                return Constants.CONTENT_TYPE_HTML + ";charset=UTF-8";
            case "txt":
                return Constants.CONTENT_TYPE_TEXT + ";charset=UTF-8";
            case "json":
                return Constants.CONTENT_TYPE_JSON;
            case "xml":
                return Constants.CONTENT_TYPE_XML;
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "css":
                return "text/css;charset=UTF-8";
            case "js":
                return "application/javascript;charset=UTF-8";
            default:
                return Constants.CONTENT_TYPE_OCTET;
        }
    }

    /**
     * 私有构造方法
     */
    private HttpUtil() {
        // 工具类不允许实例化
    }
}
