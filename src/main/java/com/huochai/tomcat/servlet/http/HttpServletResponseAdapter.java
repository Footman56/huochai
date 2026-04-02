package com.huochai.tomcat.servlet.http;

import com.huochai.tomcat.core.Response;
import com.huochai.tomcat.servlet.ServletContext;
import com.huochai.tomcat.servlet.ServletOutputStream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * HttpServletResponse适配器
 *
 * @Description 将核心Response适配为HttpServletResponse
 */
public class HttpServletResponseAdapter implements HttpServletResponse {

    private final Response response;
    private String characterEncoding = "UTF-8";
    private int status = 200;
    private String contentType;
    private boolean committed = false;

    public HttpServletResponseAdapter(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    // ===== ServletResponse methods =====

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public void setBufferSize(int size) {
        // no-op
    }

    @Override
    public int getBufferSize() {
        return 8192;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return (ServletOutputStream) response.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        response.setContentType(type);
    }

    @Override
    public String getContentType() {
        return contentType;
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
    public void flushBuffer() throws IOException {
        committed = true;
    }

    @Override
    public void reset() {
        committed = false;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void resetBuffer() {
        response.resetBuffer();
    }

    // ===== HttpServletResponse methods =====

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        response.setStatus(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.status = sc;
        response.setStatus(sc);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void sendError(int sc) throws IOException {
        setStatus(sc);
        response.sendError(sc, "");
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        setStatus(sc);
        response.sendError(sc, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        setStatus(302);
        response.sendRedirect(location);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        response.setHeader(name, String.valueOf(value));
    }

    @Override
    public void setDateHeader(String name, long date) {
        response.setHeader(name, String.valueOf(date));
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        response.addHeader(name, String.valueOf(value));
    }

    @Override
    public void addDateHeader(String name, long date) {
        response.addHeader(name, String.valueOf(date));
    }

    @Override
    public String getHeader(String name) {
        return response.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return new ArrayList<>();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> result = new ArrayList<>();
        String header = response.getHeader(name);
        if (header != null) {
            result.add(header);
        }
        return result;
    }

    @Override
    public void addCookie(Cookie cookie) {
        response.addCookie(cookie.getName(), cookie.getValue());
    }
}
