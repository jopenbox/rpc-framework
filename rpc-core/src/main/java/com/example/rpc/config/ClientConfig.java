package com.example.rpc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.rpc.exception.RpcException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端配置参数,包括ip，端口，服务名，接口等信息
 */
public class ClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClientConfig.class);

    private int maxThreadCount = 10;

    private int readTimeout = 1000;

    private String tcpNoDelay = "true";

    private String reuseAddress = "true";

    private static ClientConfig clientConfig = new ClientConfig();

    private Map<String, ServiceConfig> services = null;

    private ClientConfig() {
    }

    public static ClientConfig getInstance() {
        return clientConfig;
    }

    public ServiceConfig getService(String name) {
        return services.get(name);
    }

    private volatile boolean isInit = false;

    {
        if (!isInit) {
            this.root = getRoot();
            services = this.parseService();
            isInit = true;
        }
    }

    private Document document = null;

    private Element root = null;

    private Element getRoot() {
        Document doc = getDocument();
        // xpath表达式//application表示和application名称相同, 只要名称相同则都得到
        List<Element> list = doc.selectNodes("//application");
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    private InputStream getFileStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }

    private Document getDocument() {
        InputStream is = getFileStream("config-rpc.xml");
        try {
            if (document == null) {
                SAXReader sr = new SAXReader();
                sr.setValidation(false);
                if (is == null) {
                    throw new RpcException("can not find config file...");
                }
                document = sr.read(is);
            }
        } catch (Exception e) {
            throw new RpcException("get xml file failed", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("IOException", e);
                }
            }
        }
        return document;
    }

    private Map<String, ServiceConfig> parseService() {
        Map<String, ServiceConfig> result = new HashMap<String, ServiceConfig>();
        List<Element> serviceList = (List<Element>) root.selectNodes("//service");
        for (Element serviceNode : serviceList) {
            // service name
            String name = serviceNode.attributeValue("name");
            String connectStr = serviceNode.attributeValue("connectStr");
            String maxConnection = serviceNode.attributeValue("maxConnection");
            String async = serviceNode.attributeValue("async");

            if (name == null || "".equals(name)) {
                logger.warn("configFile: a rpc service's name is empty.");
                continue;
            }
            if (connectStr == null || "".equals(connectStr)) {
                logger.warn("configFile: rpc service［{}］ has an empty interface configure.", name);
                continue;
            }
            ServiceConfig service = new ServiceConfig(name, connectStr, maxConnection, async);
            result.put(name, service);
        }
        return result;
    }

    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
    }

    public String getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(String tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public String getReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(String reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

}
