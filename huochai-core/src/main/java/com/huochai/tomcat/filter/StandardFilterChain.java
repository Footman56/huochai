package com.huochai.tomcat.filter;

import com.huochai.tomcat.servlet.ServletRequest;
import com.huochai.tomcat.servlet.ServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准Filter链实现
 *
 * @Description 管理Filter链的执行顺序
 * @DesignPattern 责任链模式
 */
public class StandardFilterChain implements FilterChain {

    private final List<Filter> filters = new ArrayList<>();
    private int position = 0;
    private Object servlet;

    public StandardFilterChain() {
    }

    public void setServlet(Object servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
        if (position < filters.size()) {
            Filter filter = filters.get(position++);
            try {
                filter.doFilter(request, response, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (servlet != null) {
            try {
                ((com.huochai.tomcat.servlet.Servlet) servlet).service(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    public void reset() {
        position = 0;
    }

    public boolean hasNext() {
        return position < filters.size() || servlet != null;
    }
}
