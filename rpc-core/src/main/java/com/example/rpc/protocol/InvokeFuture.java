package com.example.rpc.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.example.rpc.client.filter.GenericFilter;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.example.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 封装请求和响应的桥梁
 */
public class InvokeFuture {

    private static final Logger logger = LoggerFactory.getLogger(GenericFilter.class);

    private Channel channel;
    private RpcRequest request;
    private RpcResponse response;

    private static Map<String, InvokeFuture> invokeMap = new ConcurrentHashMap<String, InvokeFuture>();

    private CountDownLatch cdl = new CountDownLatch(1);

    /**
     * 发送请求
     */
    public void send() {
        ChannelFuture writeFuture = channel.write(request);
        boolean ret = writeFuture.awaitUninterruptibly(1000, TimeUnit.MILLISECONDS);
        if (ret && writeFuture.isSuccess()) {
            return;
        }
        if (writeFuture.getCause() != null) {
            invokeMap.remove(request.getRequestID());
            throw new RpcException(writeFuture.getCause());
        } else {
            invokeMap.remove(request.getRequestID());
            throw new RpcException("sendRequest error");
        }
    }

    public RpcResponse get(long awaitTime) {
        boolean isOverTime = false;
        try {
            isOverTime = cdl.await(awaitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            invokeMap.remove(request.getRequestID());
            throw new RpcException("InterruptedException", e);
        }
        if (isOverTime) {
            invokeMap.remove(request.getRequestID());
            return response;
        } else {
            invokeMap.remove(request.getRequestID());
            throw new RpcException("sendRequest overtime");
        }
    }

    public boolean isDone() {
        return cdl.getCount() == 0;
    }

    private void doReceive(RpcResponse response) {
        this.response = response;
        cdl.countDown();
    }

    public static void receive(Channel channel, RpcResponse response) {
        InvokeFuture future = invokeMap.remove(response.getRequestID());
        if (future != null) {
            future.doReceive(response);
        } else {
            throw new RpcException("TimeOut");
        }
    }

    public InvokeFuture(Channel channel, RpcRequest request) {
        this.channel = channel;
        this.request = request;
        invokeMap.put(request.getRequestID(), this);
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public RpcRequest getRequest() {
        return request;
    }

    public void setRequest(RpcRequest request) {
        this.request = request;
    }

    public RpcResponse getResponse() {
        return response;
    }

    public void setResponse(RpcResponse response) {
        this.response = response;
    }
}
