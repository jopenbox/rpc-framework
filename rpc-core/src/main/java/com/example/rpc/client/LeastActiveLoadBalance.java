package com.example.rpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.example.rpc.config.ServiceConfig;
import com.example.rpc.connect.NettyRpcConnection;
import com.example.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.config.ClientConfig;
import com.example.rpc.connect.IRpcConnection;

/**
 * 最少活跃调用数, 相同活跃数的随机，活跃数指调用前后计数差。
 *
 * 使慢的提供者收到更少请求, 因为越慢的提供者的调用前后计数差会越大。
 * 常见的负载均衡策略有很多, 比如: 随机, 轮循, 最少活跃调用等, 开发者可以自己扩展实现
 */
public class LeastActiveLoadBalance implements ILoadBlance {

    private static final Logger logger = LoggerFactory.getLogger(LeastActiveLoadBalance.class);

    /**
     * 当前并发量
     */
    private final AtomicInteger curTotalCount = new AtomicInteger(0);

    private final static Random RANDOM_NUM = new Random();

    /**
     * 修改curTotalMap需要加锁
     */
    private final ReadWriteLock lockTotalMap = new ReentrantReadWriteLock();

    private final ReadWriteLock lockconnectionMap = new ReentrantReadWriteLock();

    /**
     * key代表serviceName,value Map中的key代表连接串,value代表该连接的当前并发数
     */
    private final Map<String, Map<String, AtomicInteger>> curTotalMap = new ConcurrentHashMap<String, Map<String, AtomicInteger>>();

    /**
     * 长连接池
     */
    private final Map<String, IRpcConnection> connectionMap = new ConcurrentHashMap<String, IRpcConnection>();

    @Override
    public int getCurTotalCount() {
        return curTotalCount.get();
    }

    @Override
    public IRpcConnection getConnection(String conn) {
        IRpcConnection connection = connectionMap.get(conn);
        if (connection == null) {
            try {
                lockconnectionMap.writeLock().lock();
                connection = connectionMap.get(conn);
                // 双重检查，避免重复创建连接
                if (connection == null) {
                    try {
                        connection = new NettyRpcConnection(conn);
                        connectionMap.put(conn, connection);
                        // 初始化连接
                        connection.open();
                        // 连接
                        connection.connect();
                    } catch (Throwable t) {
                        throw new RpcException(t);
                    }
                }
            } catch (Throwable t) {
                throw new RpcException(t);
            } finally {
                lockconnectionMap.writeLock().unlock();
            }
        }
        return connection;
    }

    @Override
    public String getLoadBlance(String serviceName) {
        if (curTotalMap.get(serviceName) == null) {
            lockTotalMap.writeLock().lock();
            try {
                if (curTotalMap.get(serviceName) == null) {
                    ClientConfig clientConfig = ClientConfig.getInstance();
                    ServiceConfig serviceConfig = clientConfig.getService(serviceName);
                    Map<String, AtomicInteger> map = new ConcurrentHashMap<String, AtomicInteger>();
                    String[] connStrs = serviceConfig.getConnectStr().split(",");
                    for (String connStr : connStrs) {
                        map.put(connStr, new AtomicInteger(0));
                    }
                    curTotalMap.put(serviceName, map);
                }
            } catch (Throwable t) {
                throw new RpcException(t);
            } finally {
                lockTotalMap.writeLock().unlock();
            }
        }
        String connStr = getMin(curTotalMap.get(serviceName));
        if (connStr != null) {
            curTotalCount.incrementAndGet();
        } else {
            throw new RpcException("the service have no alive connection,service:" + serviceName);
        }
        return connStr;
    }

    @Override
    public void finishLoadBlance(String serviceName, String connStr) {
        curTotalCount.decrementAndGet();
        curTotalMap.get(serviceName).get(connStr).decrementAndGet();
    }

    /**
     * 得到存活的并且tps最少的连接
     *
     * @param map
     * @return
     */
    private String getMin(Map<String, AtomicInteger> map) {
        if (map == null || map.size() <= 0) {
            return null;
        }
        String result = null;
        TreeMap<Integer, String> sortedMap = new TreeMap<Integer, String>();
        List<String> zeroResults = new ArrayList<String>();

        for (Entry<String, AtomicInteger> entry : map.entrySet()) {
            IRpcConnection connection = connectionMap.get(entry.getKey());
            // 为空或者已经连接
            if (connection == null || (connection != null && connection.isConnected())) {
                int cnt = entry.getValue().get();
                if (cnt == 0) {
                    String tmpResult = entry.getKey();
                    zeroResults.add(tmpResult);
                } else {
                    sortedMap.put(entry.getValue().get(), entry.getKey());
                }
            }
        }

        int zsize = zeroResults.size();
        if (zsize > 0) {
            if (zsize == 1) {
                result = zeroResults.get(0);
            } else {
                result = zeroResults.get(RANDOM_NUM.nextInt(zsize));
            }
            return result;
        } else if (sortedMap.size() >= 1) {
            result = sortedMap.firstEntry().getValue();
        } else {
            return null;
        }
        int lessCnt = map.get(result).incrementAndGet();
        int totalCnt = curTotalCount.get();
        if (totalCnt >= 10) {
            logger.warn("the concurrent connection:{}, lessCnt:{}, totalCnt:{} (>=10)", result, lessCnt, totalCnt);
        }
        logger.info("the concurrent connection:{}, lessCnt:{}, totalCnt:{}", result, lessCnt, totalCnt);
        return result;
    }
}
