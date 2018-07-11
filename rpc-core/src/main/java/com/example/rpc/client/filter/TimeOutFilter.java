package com.example.rpc.client.filter;

import com.example.rpc.client.ILoadBlance;
import com.example.rpc.util.Constants;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 监控服务性能, 如果超时则输出监控日志
 */
public class TimeOutFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TimeOutFilter.class);

    private Filter next;

    /**
     * @param next 下一个Filter
     */
    public TimeOutFilter(Filter next) {
        this.next = next;
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request, ILoadBlance loadBlance, String serviceName) throws Throwable {
        long start = System.currentTimeMillis();
        RpcResponse response = next.sendRequest(request, loadBlance, serviceName);
        long spendTime = System.currentTimeMillis() - start;
        if (spendTime > Constants.TIMEOUT_LOG_MILLSECOND) {
            logger.warn("spend time {} is bigger than {}, the serviceName is:{}",
                    spendTime, Constants.TIMEOUT_LOG_MILLSECOND, serviceName);
        }
        return response;
    }
}
