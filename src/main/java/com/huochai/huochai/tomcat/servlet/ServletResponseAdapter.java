package com.huochai.huochai.tomcat.servlet;

import com.huochai.huochai.tomcat.core.Response;
import com.huochai.huochai.tomcat.util.Constants;

import java.io.PrintWriter;

/**
 * Response适配器
 *
 * @Description 将核心Response适配为ServletResponse
 */
public class ServletResponseAdapter implements ServletResponse {

    private final Response response;
    private String characterEncoding = Constants.DEFAULT_CHARSET;

    public ServletResponseAdapter(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    @Override
    public ServletContext getServletContext() {
        if (response.getContainer() instanceof ServletContext) {
            return (ServletContext) response.getContainer();
        }
        return null;
    }

    @Override
    public void setBufferSize(int size) {
        // 简化实现
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public java.io.OutputStream getOutputStream() {
        return response.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws java.io.IOException {
        return response.getWriter();
    }

    @Override
    public void setContentType(String type) {
        response.setContentType(type);
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
        response.setCharacterEncoding(charset);
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    @Override
    public void flushBuffer() {
        try {
            response.flushBuffer();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reset() {
        response.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }
}
