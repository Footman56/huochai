package com.huochai.tomcat.servlet.http;

import com.huochai.tomcat.core.Request;
import com.huochai.tomcat.servlet.ServletContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * HttpServletRequest适配器
 *
 * @Description 将核心Request适配为HttpServletRequest
 */
public class HttpServletRequestAdapter implements HttpServletRequest {

    private final Request request;
    private String characterEncoding = "UTF-8";
    private Map<String, Object> attributes = new HashMap<>();

    public HttpServletRequestAdapter(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }

    // ===== ServletRequest methods =====

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public InputStream getInputStream() {
        byte[] body = request.getBody();
        if (body != null) {
            return new ByteArrayInputStream(body);
        }
        return request.getInputStream();
    }

    @Override
    public BufferedReader getReader() throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
    }

    @Override
    public String getProtocol() {
        return request.getProtocol();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteAddr();
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    // ===== HttpServletRequest methods =====

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append("http://");
        url.append(request.getServerName());
        url.append(":");
        url.append(request.getServerPort());
        url.append(request.getRequestURI());
        return url;
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getServletPath() {
        return request.getServletPath();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return request.getHeaderNames();
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1;
        }
        return Long.parseLong(value);
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    @Override
    public String getParameter(String name) {
        String[] values = request.getParameterMap().get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(request.getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterMap().get(name);
    }

    @Override
    public byte[] getBody() {
        return request.getBody();
    }
}
