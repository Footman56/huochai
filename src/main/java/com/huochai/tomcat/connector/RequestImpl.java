package com.huochai.tomcat.connector;

import com.huochai.tomcat.core.Container;
import com.huochai.tomcat.core.Request;
import com.huochai.tomcat.core.Wrapper;
import com.huochai.tomcat.util.Constants;
import com.huochai.tomcat.util.HttpUtil;

import java.io.InputStream;
import java.util.*;

/**
 * Request实现
 */
public class RequestImpl implements Request {
    
    private InputStream inputStream;
    private Container container;
    private Wrapper wrapper;
    
    private String method;
    private String requestURI;
    private String queryString;
    private String protocol;
    private String serverName;
    private int serverPort;
    private String remoteAddr;
    private String contextPath;
    private String servletPath;
    private String pathInfo;
    
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String[]> parameters = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();
    private byte[] body;
    private int contentLength;
    private String contentType;
    private String characterEncoding = Constants.DEFAULT_CHARSET;
    
    public RequestImpl(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public void parse() {
        try {
            byte[] data = new byte[8192];
            int len = inputStream.read(data);
            if (len > 0) {
                body = new byte[len];
                System.arraycopy(data, 0, body, 0, len);
                
                String rawRequest = new String(body, 0, len, characterEncoding);
                parseRequest(rawRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void parseRequest(String rawRequest) {
        if (rawRequest == null || rawRequest.isEmpty()) return;
        
        String[] parts = rawRequest.split("\r\n\r\n", 2);
        String headPart = parts[0];
        String bodyPart = parts.length > 1 ? parts[1] : "";
        
        String[] lines = headPart.split("\r\n");
        if (lines.length > 0) {
            parseRequestLine(lines[0]);
            
            for (int i = 1; i < lines.length; i++) {
                parseHeader(lines[i]);
            }
        }
        
        if (!bodyPart.isEmpty()) {
            body = bodyPart.getBytes();
        }
        
        if (queryString != null && !queryString.isEmpty()) {
            Map<String, String> params = HttpUtil.parseQueryString(queryString);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parameters.put(entry.getKey(), new String[]{entry.getValue()});
            }
        }
    }
    
    private void parseRequestLine(String requestLine) {
        Map<String, String> parsed = HttpUtil.parseRequestLine(requestLine);
        this.method = parsed.get("method");
        this.requestURI = parsed.get("uri");
        this.protocol = parsed.get("protocol") != null ? parsed.get("protocol") : Constants.DEFAULT_PROTOCOL;
        
        if (requestURI != null) {
            int queryIndex = requestURI.indexOf('?');
            if (queryIndex > 0) {
                this.queryString = requestURI.substring(queryIndex + 1);
                this.requestURI = requestURI.substring(0, queryIndex);
            }
            
            this.requestURI = HttpUtil.normalizeUri(this.requestURI);
        }
    }
    
    private void parseHeader(String headerLine) {
        int colonIndex = headerLine.indexOf(':');
        if (colonIndex > 0) {
            String name = headerLine.substring(0, colonIndex).trim();
            String value = headerLine.substring(colonIndex + 1).trim();
            headers.put(name.toLowerCase(), value);
        }
    }
    
    // Getter/Setter
    public String getRequestURI() { return requestURI; }
    public String getContextPath() { return contextPath != null ? contextPath : ""; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }
    public String getServletPath() { return servletPath; }
    public void setServletPath(String servletPath) { this.servletPath = servletPath; }
    public String getPathInfo() { return pathInfo; }
    public void setPathInfo(String pathInfo) { this.pathInfo = pathInfo; }
    public String getQueryString() { return queryString; }
    public String getMethod() { return method; }
    public String getProtocol() { return protocol; }
    public String getServerName() { return serverName; }
    public int getServerPort() { return serverPort; }
    public String getRemoteAddr() { return remoteAddr; }
    public int getContentLength() { 
        String len = headers.get("content-length");
        return len != null ? Integer.parseInt(len) : 0;
    }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getBody() { return body; }
    
    public String getHeader(String name) { return headers.get(name.toLowerCase()); }
    public Enumeration<String> getHeaderNames() { return Collections.enumeration(headers.keySet()); }
    
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }
    public Map<String, String[]> getParameterMap() { return parameters; }
    
    public Object getAttribute(String name) { return attributes.get(name); }
    public void setAttribute(String name, Object value) { attributes.put(name, value); }
    
    public InputStream getInputStream() { 
        return body != null ? new java.io.ByteArrayInputStream(body) : inputStream; 
    }
    
    public Container getContainer() { return container; }
    public void setContainer(Container container) { this.container = container; }
    public Wrapper getWrapper() { return wrapper; }
    public void setWrapper(Wrapper wrapper) { this.wrapper = wrapper; }
    
    public void setRemoteAddr(String remoteAddr) { this.remoteAddr = remoteAddr; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }
}
