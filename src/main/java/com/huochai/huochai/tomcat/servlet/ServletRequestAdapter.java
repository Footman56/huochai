package com.huochai.huochai.tomcat.servlet;

import com.huochai.huochai.tomcat.core.Request;
import com.huochai.huochai.tomcat.util.Constants;
import com.huochai.huochai.tomcat.util.HttpUtil;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Request适配器
 *
 * @Description 将核心Request适配为ServletRequest
 */
public class ServletRequestAdapter implements ServletRequest {

    private final Request request;
    private String characterEncoding = Constants.DEFAULT_CHARSET;

    public ServletRequestAdapter(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null; // 简化实现
    }

    @Override
    public void setAttribute(String name, Object value) {
        request.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        // 简化实现
    }

    @Override
    public ServletContext getServletContext() {
        if (request.getContainer() instanceof ServletContext) {
            return (ServletContext) request.getContainer();
        }
        return null;
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
    public byte[] getBody() throws java.io.IOException {
        return request.getBody();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return request.getRemoteAddr(); // 简化
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }
}
