package com.example.rpc.client.filter;

import com.example.rpc.client.ILoadBlance;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * 服务调用链过滤器, 可以自定义过滤器来实现一些AOP功能
 */
public interface Filter {

    /**
     * 服务调用
     *
     * @param request     协议请求实体
     * @param loadBlance  负载均衡
     * @param serviceName 服务名
     * @return
     * @throws Throwable
     */
    RpcResponse sendRequest(RpcRequest request, ILoadBlance loadBlance, String serviceName) throws Throwable;

}