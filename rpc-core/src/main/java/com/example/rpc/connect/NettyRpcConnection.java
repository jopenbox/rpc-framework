package com.example.rpc.connect;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.rpc.util.Constants;
import com.example.rpc.protocol.InvokeFuture;
import com.example.rpc.protocol.RpcRequestEncode;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.example.rpc.util.RpcContext;
import com.example.rpc.config.ClientConfig;
import com.example.rpc.exception.RpcException;
import com.example.rpc.protocol.FutureAdapter;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.protocol.RpcResponseDecode;
import com.example.rpc.util.NamedTheadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * netty客户端长连接
 */
public class NettyRpcConnection extends SimpleChannelHandler implements IRpcConnection {

    private static final Logger logger = LoggerFactory.getLogger(NettyRpcConnection.class);

    private static final ScheduledThreadPoolExecutor executorService =
            new ScheduledThreadPoolExecutor(1, new NamedTheadFactory("ConnectionHeart"));
    private volatile long lastConnectedTime = System.currentTimeMillis();
    private InetSocketAddress inetAddr;
    private volatile Channel channel;

    // 是否已经连接的标示，初始化打开和周期检测时会设置该标示
    private volatile AtomicBoolean connected = new AtomicBoolean(false);

    /**
     * 客户端配置文件
     */
    private static final ClientConfig clientConfig = ClientConfig.getInstance();
    private ClientBootstrap bootstrap = null;

    // 处理超时事件
    //private Timer timer=null;

    @SuppressWarnings("all")
    private static final ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool(), clientConfig.getMaxThreadCount());

    public NettyRpcConnection(String connStr) {
        this.inetAddr = new InetSocketAddress(connStr.split(":")[0], Integer.parseInt(connStr.split(":")[1]));
        initReconnect();
    }

    public NettyRpcConnection(String host, int port) {
        this.inetAddr = new InetSocketAddress(host, port);
        initReconnect();
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request, boolean async) throws Throwable {
        if (!isConnected() || !channel.isConnected()) {
            throw new RpcException("not connected");
        }
        // 如果request已经超时,直接抛弃
        if (System.currentTimeMillis() - request.getAddTime().getTime() > Constants.TIMEOUT_INVOKE_MILLSECOND) {
            throw new RpcException("request timeout exception");
        }
        // 异步发送请求
        InvokeFuture invokeFuture = new InvokeFuture(channel, request);
        invokeFuture.send();
        if (async) {
            // 如果是异步，则封装context
            RpcContext.getContext().setFuture(new FutureAdapter<Object>(invokeFuture));
            return new RpcResponse();
        } else {
            // 如果是同步，则阻塞调用get方法
            RpcContext.getContext().setFuture(null);
            return invokeFuture.get(Constants.TIMEOUT_INVOKE_MILLSECOND);
        }
    }

    /**
     * 初始化连接
     */
    @Override
    public void open() throws Throwable {
        open(true);
    }

    /**
     * @param connectStatus 心跳检测状态是否正常
     * @throws Throwable
     */
    private void open(boolean connectStatus) throws Throwable {
        logger.info("open start," + getConnStr());
        bootstrap = new ClientBootstrap(factory);
        //timer = new HashedWheelTimer();
        {
            bootstrap.setOption("tcpNoDelay", Boolean.parseBoolean(clientConfig.getTcpNoDelay()));
            bootstrap.setOption("reuseAddress", Boolean.parseBoolean(clientConfig.getReuseAddress()));
            bootstrap.setOption("SO_RCVBUF", 1024 * 128);
            bootstrap.setOption("SO_SNDBUF", 1024 * 128);
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                @Override
                public ChannelPipeline getPipeline() {
                    ChannelPipeline pipeline = Channels.pipeline();
                    /*
                    int readTimeout = clientConfig.getReadTimeout();
                    if (readTimeout > 0) {
                        pipeline.addLast("timeout", new ReadTimeoutHandler(timer,
                                readTimeout, TimeUnit.MILLISECONDS));
                    }
                    */
                    pipeline.addLast("encoder", new RpcRequestEncode());
                    pipeline.addLast("decoder", new RpcResponseDecode());
                    pipeline.addLast("handler", NettyRpcConnection.this);
                    return pipeline;
                }
            });
        }
        connected.set(connectStatus);
        logger.info("open finish, {}", getConnStr());
    }

    /**
     * 初始化
     */
    private void initReconnect() {
        Runnable connectStatusCheckCommand = new Runnable() {
            @Override
            public void run() {
                try {
                    // 没有连接了
                    if (!isConnected()) {
                        try {
                            open(false);
                            // 尝试连接
                            connect();
                            connected.set(true);
                        } catch (Throwable t) {
                            throw new RpcException("connect open error, conn: " + getConnStr(), t);
                        }
                    }
                    // 有连接但是连接关闭了
                    if (isConnected() && isClosed()) {
                        try {
                            connect();
                        } catch (Throwable t) {
                            throw new RpcException("connect error, conn: " + getConnStr(), t);
                        }
                    }
                    // 有连接且没有关闭
                    if (isConnected() && !isClosed()) {
                        lastConnectedTime = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - lastConnectedTime > Constants.TIMEOUT_HEARTBEAT_MILLSECOND) {
                        if (connected.get()) {
                            connected.set(false);
                            logger.error("connected has loss heartbeat, conn:{}", getConnStr());
                        }
                    }
                } catch (Throwable t) {
                    throw new RpcException("connectStatusCheckCommand error", t);
                }
            }
        };
        // 1秒发送一次心跳
        executorService.scheduleAtFixedRate(connectStatusCheckCommand, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试连接
     */
    @Override
    public void connect() {
        ChannelFuture future = bootstrap.connect(inetAddr);
        try {
            // 等待连接创建成功
            boolean ret = future.awaitUninterruptibly(Constants.TIMEOUT_CONNECTION_MILLSECOND, TimeUnit.MILLISECONDS);
            if (ret && future.isSuccess()) {
                // 获取通道
                Channel newChannel = future.getChannel();
                newChannel.setInterestOps(Channel.OP_READ_WRITE);
                try {
                    // 关闭旧的连接
                    Channel oldChannel = NettyRpcConnection.this.channel;
                    if (oldChannel != null) {
                        logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                        oldChannel.close();
                    }
                } catch (Exception e) {
                    throw new RpcException(e);
                } finally {
                    // 没有连接了
                    if (!isConnected()) {
                        try {
                            logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                            newChannel.close();
                        } catch (Throwable e) {
                            logger.error("connect fail, Exception Message:{}", e.getMessage(), e);
                        } finally {
                            NettyRpcConnection.this.channel = null;
                        }
                    } else {
                        NettyRpcConnection.this.channel = newChannel;
                    }
                }
            } else if (future.getCause() != null) {
                throw new RpcException("connect fail", future.getCause());
            } else {
                throw new RpcException("connect fail, connstr:" + this.getConnStr());
            }
        } catch (Throwable t) {
            throw new RpcException("connect fail", t);
        } finally {
            if (!isConnected()) {
                future.cancel();
            }
        }
    }

    /**
     * 客户端接受并处理消息
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        // 业务逻辑开始掺入
        RpcResponse response = (RpcResponse) e.getMessage();
        InvokeFuture.receive(channel, response);
    }

    @Override
    public void close() throws Throwable {
        connected.set(false);
        /*
        if (null != timer) {
            timer.stop();
            timer = null;
        }
        */
        if (null != channel) {
            channel.close().awaitUninterruptibly();
            channel.getFactory().releaseExternalResources();

            synchronized (channel) {
                channel.notifyAll();
            }
            channel = null;
        }
    }

    /**
     * @return true-已经连接 false-空闲
     */
    @Override
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * @return true-已经关闭连接
     */
    @Override
    public boolean isClosed() {
        return (null == channel) || !channel.isConnected()
                || !channel.isReadable() || !channel.isWritable();
    }

    public String getConnStr() {
        return inetAddr.getHostName() + ":" + inetAddr.getPort();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        super.exceptionCaught(ctx, e);
        logger.error("exceptionCaught", e.getCause());
    }
}
