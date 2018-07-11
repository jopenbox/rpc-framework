package com.example.rpc.util;

import java.util.concurrent.Future;

/**
 * RPC上下文对象
 */
public class RpcContext {

    private static final ThreadLocal<RpcContext> LOCAL = new ThreadLocal<RpcContext>() {
        @Override
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    public static RpcContext getContext() {
        return LOCAL.get();
    }

    private Future<?> future = null;

    @SuppressWarnings("unchecked")
    public <T> Future<T> getFuture() {
        return (Future<T>) future;
    }

    public void setFuture(Future<?> future) {
        this.future = future;
    }
}
