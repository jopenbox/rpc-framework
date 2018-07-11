package com.example.rpc.protocol;

import com.example.rpc.util.Constants;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 适配器，客户端只需要依赖Future接口，而服务端依赖的是InvokeFuture
 */
public class FutureAdapter<V> implements Future<V> {

    private InvokeFuture invokeFuture = null;

    public FutureAdapter(InvokeFuture invokeFuture) {
        this.invokeFuture = invokeFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return invokeFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return (V) invokeFuture.get(Constants.TIMEOUT_INVOKE_MILLSECOND).getResult();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        int timeoutInMillis = (int) unit.convert(timeout, TimeUnit.MILLISECONDS);
        return (V) invokeFuture.get(timeoutInMillis).getResult();
    }
}
