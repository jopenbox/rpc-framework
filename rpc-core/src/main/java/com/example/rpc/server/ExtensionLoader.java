package com.example.rpc.server;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.example.rpc.annotation.ServiceAnnotation;
import com.example.rpc.exception.RpcException;
import com.example.rpc.util.Constants;
import com.example.rpc.util.ClassUtil;
import com.example.rpc.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加载所有的服务
 */
public class ExtensionLoader {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    /**
     * 服务实体缓存
     */
    private static final ConcurrentMap<String, Object> cachedInstances = new ConcurrentHashMap<String, Object>();

    /**
     * 标示是否已经初始化
     */
    private static volatile boolean isInit = false;

    public static void init() {
        init(null);
    }

    private static void init(String path) {
        if (!isInit) {
            isInit = true;
            logger.info("init root path:" + path);
            try {
                scan(path);
            } catch (Exception e) {
                throw new RpcException("init error.", e);
            }
        } else {
            logger.warn("the scan have already inited.");
        }
    }

    public static Object getProxy(String serviceName) {
        if (!isInit) {
            init();
        }
        return cachedInstances.get(serviceName);
    }

    /**
     * scan jars create ContractInfo
     *
     * @param path
     * @throws Exception
     */
    private static void scan(String path) throws Exception {
        logger.info("begin scan jar from path:" + path);
        List<String> jarPathList = null;
        if (path == null) {
            String classpath = System.getProperty("java.class.path");
            String[] paths = classpath.split(Constants.PATH_SEPARATOR);
            jarPathList = FileUtil.getFirstPath(paths);
        } else {
            jarPathList = FileUtil.getFirstPath(path);
        }
        if (jarPathList == null) {
            throw new Exception("no jar fonded from path: " + path);
        }

        for (String jpath : jarPathList) {
            Set<Class> clsSet = new HashSet<Class>();
            if (jpath.endsWith(".class")) {
                // 添加到classes
                String className = jpath.substring(0, jpath.length() - 6).replace(Constants.FILE_SEPARATOR, ".");
                try {
                    Class c = Thread.currentThread().getContextClassLoader().loadClass(className);
                    clsSet.add(c);
                } catch (Exception e) {
                    throw new RpcException("class not found, class:" + jpath, e);
                }
            } else {
                try {
                    clsSet = ClassUtil.getClassFromJar(jpath);
                } catch (Exception e) {
                    throw new RpcException("getClassFromJar", e);
                }
            }
            if (clsSet == null) {
                continue;
            }

            for (Class<?> cls : clsSet) {
                try {
                    ServiceAnnotation behavior = cls.getAnnotation(ServiceAnnotation.class);
                    if (behavior != null) {
                        Object instance = cls.newInstance();
                        String serviceName = behavior.name();
                        if (null == serviceName || "".equals(serviceName)) {
                            serviceName = cls.getSimpleName();
                        }
                        cachedInstances.put(serviceName, instance);
                    }
                } catch (Exception e) {
                    throw new RpcException(e);
                }
            }
        }
        logger.info("finish scan jar.");
    }

    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }
}
