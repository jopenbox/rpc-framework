package com.example.rpc.client.filter;

import com.example.rpc.client.ILoadBlance;
import com.example.rpc.config.ServiceConfig;
import com.example.rpc.connect.IRpcConnection;
import com.example.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.config.ClientConfig;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * 最终执行的Filter, 得到socket长连接并发送请求
 */
public class GenericFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(GenericFilter.class);

    @Override
    public RpcResponse sendRequest(RpcRequest request, ILoadBlance loadBlance, String serviceName) throws Throwable {
        IRpcConnection connection = null;
        RpcResponse response = null;
        // 根据服务名得到服务连接字符串
        String connStr = loadBlance.getLoadBlance(serviceName);
        // 客户端配置
        ClientConfig clientConfig = ClientConfig.getInstance();
        // 客户端单个服务配置
        ServiceConfig serviceConfig = clientConfig.getService(serviceName);

        try {
            connection = loadBlance.getConnection(connStr);
            if (connection.isConnected() && connection.isClosed()) {
                connection.connect();
            }
            if (connection.isConnected() && !connection.isClosed()) {
                response = connection.sendRequest(request, serviceConfig.getAsync());
            } else {
                throw new RpcException("send rpc request fail");
            }
            return response;
        } catch (RpcException e) {
            logger.error("send rpc request fail! RpcException Message:{}", e.getMessage(), e);
            throw e;
        } catch (Throwable t) {
            logger.warn("send rpc request fail! request:{}", request, t);
            throw new RpcException(t);
        } finally {
            loadBlance.finishLoadBlance(serviceName, connStr);
        }
    }
}
