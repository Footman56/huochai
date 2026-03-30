package com.huochai.huochai.tomcat.connector;

import com.huochai.huochai.tomcat.core.Engine;
import com.huochai.huochai.tomcat.core.Request;
import com.huochai.huochai.tomcat.core.Response;
import com.huochai.huochai.tomcat.util.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 连接器
 *
 * @Description 监听端口，接受客户端连接，委托给容器处理请求
 * @DesignPattern 外观模式 - 为服务器隐藏了请求处理的复杂性
 */
public class Connector implements Runnable {

    /** 服务器端口 */
    private int port = Constants.DEFAULT_PORT;

    /** 服务器套接字 */
    private ServerSocket serverSocket;

    /** 运行状态 */
    private boolean running = false;

    /** 线程池 */
    private ExecutorService threadPool;

    /** 引擎 */
    private Engine engine;

    /**
     * 构造方法
     *
     * @param port 端口
     * @param engine 引擎
     */
    public Connector(int port, Engine engine) {
        this.port = port;
        this.engine = engine;
        this.threadPool = Executors.newFixedThreadPool(Constants.MAX_CONNECTIONS);
    }

    /**
     * 启动连接器
     */
    public void start() throws IOException {
        if (!running) {
            serverSocket = new ServerSocket(port);
            running = true;

            Thread thread = new Thread(this);
            thread.setName("Connector-" + port);
            thread.start();

            System.out.println("Connector started on port " + port);
        }
    }

    /**
     * 停止连接器
     */
    public void stop() {
        if (running) {
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                threadPool.shutdown();
                System.out.println("Connector stopped on port " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 监听连接
     */
    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(Constants.DEFAULT_TIMEOUT);

                threadPool.execute(new RequestHandler(clientSocket, engine));
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    /**
     * 请求处理器
     */
    private static class RequestHandler implements Runnable {

        private Socket socket;
        private Engine engine;

        public RequestHandler(Socket socket, Engine engine) {
            this.socket = socket;
            this.engine = engine;
        }

        @Override
        public void run() {
            try {
                // 创建请求和响应对象
                Request request = new RequestImpl(socket.getInputStream());
                Response response = new ResponseImpl(socket.getOutputStream());

                // 设置请求信息
                request.setRemoteAddr(socket.getInetAddress().getHostAddress());
                request.setServerPort(socket.getLocalPort());
                request.setServerName(socket.getLocalAddress().getHostName());
                request.setContainer(engine);

                // 解析请求
                request.parse();

                // 通过Engine处理请求
                if (engine != null) {
                    engine.getPipeline().invoke(request, response);
                }

                // 构建并发送响应
                response.build();

                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
