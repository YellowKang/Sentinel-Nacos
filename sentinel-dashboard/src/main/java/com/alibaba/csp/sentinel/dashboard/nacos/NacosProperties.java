package com.alibaba.csp.sentinel.dashboard.nacos;

import com.alibaba.csp.sentinel.util.StringUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** @author zouwei */
@Component
@ConfigurationProperties(prefix = "nacos.server")
public class NacosProperties {
    private static final String NACOS_SERVER_ADDR = "NACOS_SERVER_ADDR";
    private static final String NACOS_GROUP_ID = "NACOS_GROUP_ID";
    private static final String NACOS_NAMESPACE = "NACOS_NAMESPACE";
    private static final String NACOS_PORT = "NACOS_PORT";
    private static final String NACOS_IP = "NACOS_IP";

    private String ip = "localhost";

    private String port = "8848";

    private String namespace;

    private String groupId = "DEFAULT_GROUP";

    private boolean enable = false;

    public boolean isOpen(){
        return enable;
    }

    public String getIp() {
        String nacosIp = getEnv(NACOS_IP);
        if (StringUtil.isNotBlank(nacosIp)) {
            return nacosIp;
        }
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        String nacosPort = getEnv(NACOS_PORT);
        if (StringUtil.isNotBlank(nacosPort)) {
            return nacosPort;
        }
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getNamespace() {
        String nacosNamespace = getEnv(NACOS_NAMESPACE);
        if (StringUtil.isNotBlank(nacosNamespace)) {
            return nacosNamespace;
        }
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGroupId() {
        String nacosGroupId = getEnv(NACOS_GROUP_ID);
        if (StringUtil.isNotBlank(nacosGroupId)) {
            return nacosGroupId;
        }
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getServerAddr() {
        String nacosServerAddr = getEnv(NACOS_SERVER_ADDR);
        if (StringUtil.isNotBlank(nacosServerAddr)) {
            return nacosServerAddr;
        }
        return this.getIp() + ":" + this.getPort();
    }

    private static String getEnv(String key) {
        return System.getenv(key);
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
