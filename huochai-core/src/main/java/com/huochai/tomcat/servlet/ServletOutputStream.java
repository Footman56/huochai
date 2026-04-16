package com.huochai.tomcat.servlet;

import java.io.OutputStream;
import java.io.IOException;

/**
 * ServletOutputStream抽象类
 *
 * @Description 提供Servlet输出流的基础实现
 */
public abstract class ServletOutputStream extends OutputStream {

    public boolean isReady() {
        return true;
    }

    public void setWriteListener(WriteListener listener) {
        // Default no-op implementation
    }

    /**
     * WriteListener接口
     */
    public interface WriteListener {
        void onWritePossible() throws IOException;
        void onError(Throwable t);
    }
}
