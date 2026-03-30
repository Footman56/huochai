package com.huochai.huochai.tomcat.pipeline;

import com.huochai.huochai.tomcat.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准管道实现
 * 
 * @Description 提供Pipeline接口的默认实现
 * @DesignPattern 责任链模式
 */
public class StandardPipeline implements Pipeline {
    
    // ==================== 基本属性 ====================
    
    private final Container container;
    private Valve basic;
    private final List<Valve> valves = new ArrayList<>();
    private volatile boolean started = false;
    
    // ==================== 构造方法 ====================
    
    public StandardPipeline(Container container) {
        this.container = container;
    }
    
    // ==================== Pipeline 接口实现 ====================
    
    @Override
    public Valve getBasic() {
        return basic;
    }
    
    @Override
    public void setBasic(Valve valve) {
        this.basic = valve;
        // 标记为基础阀门
        if (valve instanceof StandardValve) {
            ((StandardValve) valve).setBasic(true);
        }
    }
    
    @Override
    public void addValve(Valve valve) {
        if (valve == null) {
            return;
        }
        
        // 确保阀门名称唯一
        String valveName = valve.getName();
        if (valveName == null || valveName.isEmpty()) {
            valve.setName(valve.getClass().getSimpleName() + "_" + valves.size());
        }
        
        valves.add(valve);
        
        // 如果管道已启动，同步启动阀门
        if (started) {
            try {
                valve.start();
            } catch (Exception e) {
                container.log("Valve start failed: " + valve.getName(), e);
            }
        }
    }
    
    @Override
    public void removeValve(Valve valve) {
        if (valve == null) {
            return;
        }
        
        synchronized (valves) {
            valves.remove(valve);
        }
        
        // 如果管道已启动，同步停止阀门
        if (started && valve.isStarted()) {
            try {
                valve.stop();
            } catch (Exception e) {
                container.log("Valve stop failed: " + valve.getName(), e);
            }
        }
    }
    
    @Override
    public Valve[] getValves() {
        synchronized (valves) {
            return valves.toArray(new Valve[0]);
        }
    }
    
    @Override
    public void invoke(Request request, Response response) {
        // 创建阀门上下文
        StandardValveContext context = new StandardValveContext();
        
        // 从第一个阀门开始调用
        Valve[] valves = getValves();
        if (valves.length > 0) {
            context.invokeNext(valves, 0, request, response);
        } else if (basic != null) {
            // 没有普通阀门，直接调用基础阀门
            basic.invoke(request, response, context);
        } else {
            // 没有阀门，直接返回
            // (这种情况不应该发生)
        }
    }
    
    @Override
    public void removeAllValves() {
        synchronized (valves) {
            for (Valve valve : valves) {
                try {
                    if (valve.isStarted()) {
                        valve.stop();
                    }
                } catch (Exception e) {
                    container.log("Valve stop failed: " + valve.getName(), e);
                }
            }
            valves.clear();
        }
    }
    
    // ==================== 生命周期 ====================
    
    public void init() {
        // 初始化所有阀门
        for (Valve valve : valves) {
            try {
                valve.init();
            } catch (Exception e) {
                container.log("Valve init failed: " + valve.getName(), e);
            }
        }
        
        // 初始化基础阀门
        if (basic != null) {
            try {
                basic.init();
            } catch (Exception e) {
                container.log("Basic valve init failed", e);
            }
        }
    }
    
    public void start() throws Exception {
        started = true;
        
        // 启动所有阀门
        for (Valve valve : valves) {
            try {
                valve.start();
            } catch (Exception e) {
                container.log("Valve start failed: " + valve.getName(), e);
            }
        }
        
        // 启动基础阀门
        if (basic != null) {
            try {
                basic.start();
            } catch (Exception e) {
                container.log("Basic valve start failed", e);
            }
        }
    }
    
    public void stop() throws Exception {
        // 停止所有阀门
        for (Valve valve : valves) {
            try {
                if (valve.isStarted()) {
                    valve.stop();
                }
            } catch (Exception e) {
                container.log("Valve stop failed: " + valve.getName(), e);
            }
        }
        
        // 停止基础阀门
        if (basic != null) {
            try {
                if (basic.isStarted()) {
                    basic.stop();
                }
            } catch (Exception e) {
                container.log("Basic valve stop failed", e);
            }
        }
        
        started = false;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    // ==================== 阀门上下文 ====================
    
    /**
     * 标准阀门上下文实现
     */
    private class StandardValveContext implements Valve.ValveContext {
        
        private int position = 0;
        
        public void invokeNext(Valve[] valves, int pos, Request request, Response response) {
            if (pos < valves.length) {
                this.position = pos;
                valves[pos].invoke(request, response, this);
            } else if (basic != null) {
                // 所有阀门处理完毕，调用基础阀门
                basic.invoke(request, response, this);
            }
        }
        
        @Override
        public void invokeNext(Request request, Response response) {
            Valve[] valves = StandardPipeline.this.getValves();
            if (position < valves.length) {
                invokeNext(valves, position, request, response);
            } else if (basic != null) {
                basic.invoke(request, response, this);
            }
        }
    }
}
