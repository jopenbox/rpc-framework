package com.example.server.client;

import com.example.rpc.client.RpcClientProxy;
import com.example.rpc.util.RpcContext;
import com.example.server.domain.Message;
import com.example.server.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        try {
            final IService server = RpcClientProxy.proxy(IService.class, "myService", "myService");

            long startMillis = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                final int f_i = i;
                send(server, f_i);
            }
            long endMillis = System.currentTimeMillis();
            logger.info("spend time:{}", (endMillis - startMillis));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息
     *
     * @param service
     * @param f_i
     */
    private static void send(IService service, int f_i) {
        Message msg = null;
        try {
            // 由于客户端配置的async="true",我们用异步方式来获取结果,如果是同步方式,直接msg=server.echoMsg(f_i)即可
            // 调用rpc服务的方法
            service.echoMsg(f_i);

            Future<Message> future = RpcContext.getContext().getFuture();
            // 返回值不为null则打印输出
            if (future != null) {
                msg = future.get();
                logger.info("------ msg:{}, {}", msg.getMsg(), msg.getData());
            }
        } catch (Throwable e) {
            logger.error("client send fail!", e);
        }
    }
}
