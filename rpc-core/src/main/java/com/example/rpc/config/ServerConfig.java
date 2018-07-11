package com.example.rpc.config;

/**
 * 服务启动配置, 参数有线程数目、超时时间、端口等
 */
public class ServerConfig {

    /**
     * 线程数
     */
    private int threadCnt = 100;

    /**
     * 读取数据超时时间,毫秒
     */
    private int readTimeout = 10000;

    /**
     * 连接超时时间,毫秒
     */
    private int connectTimeout = 1000;

    /**
     * 默认端口
     */
    private int port = 9090;

    public ServerConfig(int port) {
        this.port = port;
    }

    public int getThreadCnt() {
        return threadCnt;
    }

    public void setThreadCnt(int threadCnt) {
        this.threadCnt = threadCnt;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

}
