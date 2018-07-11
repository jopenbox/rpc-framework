package com.example.rpc.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.example.rpc.config.ServerConfig;
import com.example.rpc.protocol.RpcRequestDecode;
import com.example.rpc.protocol.RpcResponseEncode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rpc服务实现类，在此开启长连接
 */
public class RpcServerBootstrap implements IRpcServer {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerBootstrap.class);

    /**
     * 负责初始化netty服务器,并且开始监听端口的socket请求,用于接受客户端的连接以及为已接受的连接创建子通道
     */
    private ServerBootstrap bootstrap = null;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * 处理超时事件
     */
    private Timer timer = null;

    /**
     * 初始化
     *
     * @param myport 端口
     */
    @SuppressWarnings("all")
    private void initHttpBootstrap(int myport) {
        logger.info("initHttpBootstrap, port:{}", myport);
        final ServerConfig serverConfig = new ServerConfig(myport);
        // ChannelGroup可管理服务器端所有的连接的Channel(通道),然后对所有的连接Channel广播消息
        final ChannelGroup channelGroup = new DefaultChannelGroup(getClass().getName());

        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss线程池
                Executors.newCachedThreadPool(), // worker线程池
                serverConfig.getThreadCnt()));
        // 设置常见参数
        bootstrap.setOption("tcpNoDelay", "true");
        bootstrap.setOption("reuseAddress", "true");
        bootstrap.setOption("SO_RCVBUF", 1024 * 128);
        bootstrap.setOption("SO_SNDBUF", 1024 * 128);
        // netty定时器
        timer = new HashedWheelTimer();
        // 注册ChannelPipelineFactory
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                /* 在数据处理管道实现类里配置空闲状态处理代码 */
                ChannelPipeline pipeline = Channels.pipeline();

                int readTimeout = serverConfig.getReadTimeout();
                if (readTimeout > 0) {
                    // 设置读取数据超时处理
                    pipeline.addLast("readTimeOut", new ReadTimeoutHandler(timer,
                            readTimeout, TimeUnit.MILLISECONDS)); // 毫秒
                }

                pipeline.addLast("decoder", new RpcRequestDecode());
                pipeline.addLast("encoder", new RpcResponseEncode());
                pipeline.addLast("handler", new NettyRpcServerHandler(channelGroup));

                return pipeline;
            }
        });

        int port = serverConfig.getPort();
        if (!checkPortConfig(port)) {
            throw new IllegalStateException("port: " + port + " already in use!");
        }

        // 端口开始监听,等待客户端来连接
        Channel channel = bootstrap.bind(new InetSocketAddress(port));
        channelGroup.add(channel);
        logger.info("rpc-framework server started...........");

        waitForShutdownCommand();
        ChannelGroupFuture future = channelGroup.close();
        future.awaitUninterruptibly();
        bootstrap.releaseExternalResources();
        timer.stop();
        timer = null;

        logger.info("rpc-framework server stoped...........");
    }

    @Override
    public void start(int port) {
        ExtensionLoader.init();
        initHttpBootstrap(port);
    }

    @Override
    public void stop() {
        stopped.set(true);
        synchronized (stopped) {
            stopped.notifyAll();
        }
    }

    /**
     * 等待停止命令
     */
    private void waitForShutdownCommand() {
        synchronized (stopped) {
            while (!stopped.get()) {
                try {
                    stopped.wait();
                } catch (InterruptedException e) {
                    logger.error("waitForShutdownCommand InterruptedException Message:{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 检查端口配置是否可用
     *
     * @param listenPort
     * @return true-可用
     */
    private boolean checkPortConfig(int listenPort) {
        if (listenPort < 0 || listenPort > 65536) {
            throw new IllegalArgumentException("Invalid start port: " + listenPort);
        }
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(listenPort);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(listenPort);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            logger.error("checkPortConfig IOException Message:{}", e.getMessage(), e);
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    logger.error("checkPortConfig IOException Message:{}", e.getMessage(), e);
                }
            }
        }
        return false;
    }
}
