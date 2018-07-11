package com.example.rpc.server;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.util.ReflectionCache;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC具体处理接口
 *
 * 拦截IO工作线程产生的事件，传输消息或者执行相关的业务逻辑
 */
public class NettyRpcServerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServerHandler.class);

    private final ChannelGroup channelGroups;

    public NettyRpcServerHandler() {
        this.channelGroups = null;
    }

    public NettyRpcServerHandler(ChannelGroup channelGroups) {
        this.channelGroups = channelGroups;
    }

    @SuppressWarnings("all")
    private static final ExecutorService WORKER_SERVICE = Executors.newFixedThreadPool(100);

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        if (null != channelGroups) {
            channelGroups.add(e.getChannel());
        }
    }

    /**
     * 捕获异常
     *
     * @param ctx
     * @param e
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        RpcRequest request = (RpcRequest) ctx.getAttachment();
        logger.error("exceptionCaught - handle rpc request fail! request: {}", request, e.getCause());
        e.getChannel().close().awaitUninterruptibly();
    }

    /**
     * 消息接收
     *
     * @param ctx
     * @param e
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof RpcRequest)) {
            logger.error("not RpcRequest received!");
            return;
        }
        final RpcRequest request = (RpcRequest) msg;
        ctx.setAttachment(request);

        // 构建协议响应实体
        final RpcResponse response = new RpcResponse(request.getRequestID());
        WORKER_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Object result = handle(request);
                    // 设置返回数据
                    response.setResult(result);
                } catch (Throwable t) {
                    logger.error("messageReceived - handle rpc request fail! request: {}", request , t);
                    response.setException(t);
                }
                // 写入通道
                e.getChannel().write(response);
            }
        });
    }

    /**
     * 处理
     *
     * @param request 协议请求实体
     * @return
     * @throws Throwable
     */
    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        Object rpcService = ExtensionLoader.getProxy(className);
        if (null == rpcService) {
            throw new NullPointerException("server interface config is null");
        }

        Method method = ReflectionCache.getMethod(request.getInterfaceName(),
                request.getMethodName(), request.getParameterTypes());
        Object[] parameters = request.getParameters();
        // 调用invoke方法执行反射调用
        return method.invoke(rpcService, parameters);
    }
}
