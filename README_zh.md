# 1.rpc-framework

一个采用Java实现的小巧的RPC框架，一共2.9K多行代码，实现了RPC的基本功能，目前具有如下特点：

    1.服务端基于注解，启动时自动扫描所有RPC实现，基本零配置;
    2.客户端实现Filter机制，可以自定义Filter;
    3.基于netty的Reactor IO多路复用网络模型;
    4.数据层提供protobuf和hessian的实现，可以扩展ISerializer接口自定义实现其他;
    5.负载均衡算法采用最少活跃调用数算法，可以扩展ILoadBlance接口自定义实现其他;
    6.客户端支持服务的同步或异步调用。

请求流程：

客户端发送请求 -> RpcClientProxy代理 -> Filter(过滤器) -> LoadBlance(负载均衡) -> Transporter传输 -> NettyRpcServerHandler(RPC具体处理接口) -> ExtensionLoader(加载所有的服务) -> 反射Method -> RPC服务端执行请求

一个通用的网络RPC框架，它应该包括如下元素：

    1.具有服务的分层设计，借鉴Future/Service/Filter概念；
    2.具有网络的分层设计，区分协议层、数据层、传输层、连接层；
    3.独立的可适配的codec层，可以灵活增加HTTP，Memcache，Redis，MySQL/JDBC，Thrift等协议的支持；
    4.将多年各种远程调用High availability的经验融入在实现中，如负载均衡，failover，多副本策略，开关降级等；
    5.通用的远程调用实现，采用async方式来减少业务服务的开销，并通过future分离远程调用与数据流程的关注；
    6.具有状态查看及统计功能；
    7.当然，最终要的是，具备以下通用的远程容错处理能力，超时、重试、负载均衡、failover……

本系统未来改进点包括：

    1.增加注册中心功能，在大项目中，一个项目可能依赖成百上千个服务，如果基于配置文件直接指定服务地址会增加维护成本，需要引入注册中心；
    2.目前用的是反射和java代理实现的服务端存根和客户端代理，为了提高性能，可以把这些用javassit，asm等java字节码工具实现；
    3.增加一些监控功能，为了增强服务的稳定性和服务的可控性，监控功能是不可或缺的；
    4.目前应用协议采用的是最简单的协议，仅仅一个魔数+序列化的实体，这些需要增强，比如增加版本号以解决向前兼容性；
    5.增加High availability的一些手段，目前只有负载均衡，其他的比如failover，多副本策略，开关降级等，过载保护等需要自己实现；

# 2.Getting started

## 1.服务端（服务提供方）

1.定义服务接口:

```java
public interface IService {
    Message echoMsg(int msg);
}
```

服务接口实现:

```java
@ServiceAnnotation(name = "myService")
public class MyService1 implements IService {
    @Override
    public Message echoMsg(int msg) {
        logger.info("MyService1 echoMsg int:" + msg);
        Message result = new Message();
        result.setMsg("int:" + msg);
        result.setData(new Date());
        return result;
    }
}
```

2.编写服务提供方测试主函数并启动:

```java
public static void main(String[] args) {
    RpcServerBootstrap bootstrap = new RpcServerBootstrap();
    bootstrap.start(10018);
}
```

## 2.客户端（服务消费方）

1.定义服务接口:

```java
public interface IService {
    Message echoMsg(int msg);
}
```

2.编写服务消费方测试主函数:

```java
public class Client {

    public static void main(String[] args) {
        try {
            final IService server = RpcClientProxy.proxy(IService.class, "myService", "myService");
            // 调用服务方法
            service.echoMsg(123);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
```

3.编写配置文件:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<application maxThreadCount="100">
    <service name="myService" connectStr="localhost:10018" maxConnection="200" async="true"/>
</application>
```

启动服务消费方测试主函数，通过控制台可以看到服务已经被消费了并返回了结果。
