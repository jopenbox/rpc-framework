package com.example.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.rpc.client.filter.Filter;
import com.example.rpc.client.filter.GenericFilter;
import com.example.rpc.client.filter.TimeOutFilter;
import com.example.rpc.client.filter.TpsFilter;
import com.example.rpc.util.Sequence;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rpc客户端动态代理类, 一般为了提高性能会使用javassist等工具直接操作Java字节码
 */
public class RpcClientProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);

    /**
     * 根据serviceName得到是哪个服务
     */
    private final String serviceName;
    /**
     * 要调用的接口
     */
    private final Class<?> interfaceClass;
    /**
     * 要调用的具体接口实现
     */
    private final String className;
    /**
     * 负载均衡策略 - 最少活跃调用数
     */
    private final ILoadBlance loadBlance = new LeastActiveLoadBalance();
    /**
     * 客户端动态代理缓存
     */
    private final static Map<String, Object> proxyMap = new ConcurrentHashMap<String, Object>();
    /**
     * 服务调用链过滤器
     */
    private final Filter lastFilter;

    /**
     * 初始化客户端动态代理类
     *
     * @param interfaceClass 接口类
     * @param serviceName    服务名
     * @param className      实现类名
     */
    private RpcClientProxy(Class<?> interfaceClass, String serviceName, String className) {
        super();
        this.serviceName = serviceName;
        this.interfaceClass = interfaceClass;
        this.className = className;
        // 最终执行的Filter
        GenericFilter genericFilter = new GenericFilter();
        // 监控服务性能
        TimeOutFilter timeOutFilter = new TimeOutFilter(genericFilter);
        lastFilter = new TpsFilter(timeOutFilter);
    }

    /**
     * 代理
     *
     * @param interfaceClass 接口类
     * @param serviceName    服务名
     * @param className      实现类名
     * @param <T>
     * @return 代理类的实例
     * @throws Throwable
     */
    public static <T> T proxy(Class<T> interfaceClass, String serviceName, String className) throws Throwable {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(interfaceClass.getName() + " is not an interface");
        }
        String key = interfaceClass.getName() + "_" + serviceName + "_" + className;
        // 从缓存中获取
        Object proxy = proxyMap.get(key);
        if (proxy == null) {
            // 动态的创建一个被代理类的一个代理类的实例
            proxy = Proxy.newProxyInstance(
                    // 获取类加载器
                    interfaceClass.getClassLoader(),
                    // 获取被代理类的所有接口信息
                    new Class<?>[]{interfaceClass},
                    new RpcClientProxy(interfaceClass, serviceName, className));
            // 加入缓存
            proxyMap.put(key, proxy);
            logger.info("proxy generated,serviceName:" + serviceName + ",className:" + className);
        }
        return (T) proxy;
    }

    /**
     * 真正调用的地方
     *
     * @param proxy  proxy生成的代理对象
     * @param method 方法名
     * @param args   被代理方法中的参数
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String interfaceName = interfaceClass.getName();
        List<String> parameterTypes = new LinkedList<String>();
        for (Class<?> parameterType : method.getParameterTypes()) {
            parameterTypes.add(parameterType.getName());
        }
        // requestID用来唯一标识一次请求
        String requestID = generateRequestID();
        // 构造协议请求实体
        final RpcRequest request = new RpcRequest(
                requestID,
                interfaceName,
                className,
                method.getName(),
                parameterTypes.toArray(new String[0]),
                args);
        // 开始走Filter逻辑,最后返回协议响应实体
        RpcResponse response = lastFilter.sendRequest(request, loadBlance, serviceName);
        return response.getResult();
    }

    /**
     * 生成requestID
     *
     * @return
     */
    private static String generateRequestID() {
        return Sequence.next() + "";
    }

}
