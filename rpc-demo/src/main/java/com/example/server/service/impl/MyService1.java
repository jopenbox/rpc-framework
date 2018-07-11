package com.example.server.service.impl;

import java.util.Date;

import com.example.rpc.server.RpcServerBootstrap;
import com.example.server.domain.Message;
import com.example.server.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.rpc.annotation.ServiceAnnotation;

/**
 * 服务接口实现类
 */
@ServiceAnnotation(name = "myService")
public class MyService1 implements IService {

    private static final Logger logger = LoggerFactory.getLogger(MyService1.class);

    public static void main(String[] args) {
        RpcServerBootstrap bootstrap = new RpcServerBootstrap();
        bootstrap.start(10018);
    }

    @Override
    public Message echoMsg(int msg) {
        logger.info("MyService1 echoMsg int:" + msg);
        Message result = new Message();
        result.setMsg("int:" + msg);
        result.setData(new Date());
        return result;
    }
}
