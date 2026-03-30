package com.huochai.huochai.tomcat.connector;

import com.huochai.huochai.tomcat.core.Container;
import com.huochai.huochai.tomcat.core.Response;
import com.huochai.huochai.tomcat.util.Constants;
import com.huochai.huochai.tomcat.util.HttpUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * Response实现
 */
public class ResponseImpl implements Response {
    
    private OutputStream outputStream;
    private Container container;
    
    private int status = Constants.SC_OK;
    private String statusMessage = "OK";
    private Map<String, List<String>> headers = new HashMap<>();
    private List<String> cookies = new ArrayList<>();
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private String contentType = Constants.DEFAULT_CONTENT_TYPE;
    private String characterEncoding = Constants.DEFAULT_CHARSET;
    private int contentLength;
    private boolean committed = false;
    private PrintWriter writer;
    private ServletOutputStreamImpl servletOutputStream;
    
    public ResponseImpl(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.servletOutputStream = new ServletOutputStreamImpl(buffer);
    }
    
    public void build() throws IOException {
        try {
            if (writer != null) {
                writer.flush();
            }

            byte[] body = buffer.toByteArray();
            System.out.println("[Debug] Response body length: " + body.length);

            if (getHeader(Constants.HEADER_CONTENT_LENGTH) == null && body.length > 0) {
                setContentLength(body.length);
            }

            StringBuilder response = new StringBuilder();
            response.append(generateResponseLine()).append("\r\n");

            for (String name : headers.keySet()) {
                List<String> values = headers.get(name);
                for (String value : values) {
                    response.append(name).append(": ").append(value).append("\r\n");
                }
            }

            for (String cookie : cookies) {
                response.append(Constants.HEADER_SET_COOKIE).append(": ").append(cookie).append("\r\n");
            }

            response.append("\r\n");

            String headerStr = response.toString();
            System.out.println("[Debug] Response headers:\n" + headerStr);
            outputStream.write(headerStr.getBytes(characterEncoding));

            if (body.length > 0) {
                System.out.println("[Debug] Writing body: " + body.length + " bytes");
                outputStream.write(body);
            }

            outputStream.flush();
            committed = true;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to build response", e);
        }
    }
    
    private String generateResponseLine() {
        return HttpUtil.generateResponseLine(Constants.DEFAULT_PROTOCOL, status);
    }
    
    // Setter/Getter
    public void setStatus(int statusCode) {
        this.status = statusCode;
        this.statusMessage = HttpUtil.getStatusMessage(statusCode);
    }
    public int getStatus() { return status; }
    public void setStatusMessage(String message) { this.statusMessage = message; }
    public String getStatusMessage() { return statusMessage; }
    
    public void sendError(int statusCode, String message) throws IOException {
        setStatus(statusCode);
        String errorPage = "<html><head><title>Error " + statusCode + "</title></head>" +
                "<body><h1>Error " + statusCode + ": " + message + "</h1></body></html>";
        byte[] body = errorPage.getBytes(characterEncoding);
        setContentLength(body.length);
        buffer.write(body);
    }
    
    public void sendRedirect(String location) throws IOException {
        setStatus(Constants.SC_FOUND);
        setHeader(Constants.HEADER_LOCATION, location);
    }
    
    public void setHeader(String name, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(name.toLowerCase(), values);
    }
    
    public void addHeader(String name, String value) {
        name = name.toLowerCase();
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name, values);
        }
        values.add(value);
    }
    
    public String getHeader(String name) {
        List<String> values = headers.get(name.toLowerCase());
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
        int charsetIndex = contentType.toLowerCase().indexOf("charset=");
        if (charsetIndex > 0) {
            this.characterEncoding = contentType.substring(charsetIndex + 8).trim();
        }
        setHeader(Constants.HEADER_CONTENT_TYPE, contentType);
    }
    public String getContentType() { return contentType; }
    
    public void setContentLength(int length) {
        this.contentLength = length;
        setIntHeader(Constants.HEADER_CONTENT_LENGTH, length);
    }
    public int getContentLength() { return contentLength; }
    
    public void setCharacterEncoding(String charset) { this.characterEncoding = charset; }
    public String getCharacterEncoding() { return characterEncoding; }
    
    public void addCookie(String name, String value) {
        cookies.add(name + "=" + value);
    }
    
    public OutputStream getOutputStream() { return servletOutputStream; }
    
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new java.io.OutputStreamWriter(buffer, characterEncoding));
        }
        return writer;
    }
    
    public boolean isCommitted() { return committed; }
    public void resetBuffer() { if (!committed) buffer.reset(); }
    public void flushBuffer() throws IOException { committed = true; }
    public byte[] getBody() { return buffer.toByteArray(); }

    public Container getContainer() { return container; }
    public void setContainer(Container container) { this.container = container; }

    private void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    private static class ServletOutputStreamImpl extends com.huochai.huochai.tomcat.servlet.ServletOutputStream {
        private final ByteArrayOutputStream buffer;

        public ServletOutputStreamImpl(ByteArrayOutputStream buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(int b) { buffer.write(b); }
        @Override
        public void write(byte[] b) throws IOException { buffer.write(b); }
        @Override
        public void write(byte[] b, int off, int len) { buffer.write(b, off, len); }
    }
}
