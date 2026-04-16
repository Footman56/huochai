package com.huochai.tomcat.session;

import com.huochai.tomcat.core.Context;
import com.huochai.tomcat.servlet.HttpSession;

import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准Session实现
 */
public class StandardSession implements HttpSession {

    private final String id;
    private final long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval = 30 * 60; // 30分钟
    private boolean valid = true;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Context context;

    public StandardSession(Context context) {
        this.context = context;
        this.id = UUID.randomUUID().toString();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public Object getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return java.util.Collections.enumeration(attributes.keySet());
    }

    @Override
    public String[] getValueNames() {
        return attributes.keySet().toArray(new String[0]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.remove(name);
        }
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        attributes.clear();
        valid = false;
    }

    @Override
    public boolean isNew() {
        return lastAccessedTime == creationTime;
    }

    public void access() {
        lastAccessedTime = System.currentTimeMillis();
    }

    public boolean isValid() {
        return valid;
    }

    public Context getContext() {
        return context;
    }
}
