package com.example.rpc.client.filter;

import com.example.rpc.client.ILoadBlance;
import com.example.rpc.util.Constants;
import com.example.rpc.exception.RpcException;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * 控制客户端调用服务的最大并发量, 超过最大并发量直接抛异常
 */
public class TpsFilter implements Filter {

    private Filter next;

    /**
     * @param next 下一个Filter
     */
    public TpsFilter(Filter next) {
        this.next = next;
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request, ILoadBlance loadBlance, String serviceName) throws Throwable {
        int maxConcurrentNum = Constants.CLIENT_CONCURRENT_NUM;
        if (loadBlance.getCurTotalCount() > maxConcurrentNum) {
            throw new RpcException("total invoke is bigger than " + maxConcurrentNum);
        } else {
            return next.sendRequest(request, loadBlance, serviceName);
        }
    }
}
