package com.alibaba.csp.sentinel.dashboard.nacos;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @Author HuangKang
 * @Date 2021/4/29 下午5:10
 * @Summarize Nacos存储配置,如果没有开启则不装配ConfigServiceBean
 */
@Configuration
@ConditionalOnProperty(value = {"nacos.server.enable"}, havingValue = "true")
public class NacosConfig {

    private final NacosProperties nacosProperties;

    @Autowired
    public NacosConfig(NacosProperties nacosProperties) {
        this.nacosProperties = nacosProperties;
    }

    @Bean
    public ConfigService configService() throws Exception {
        Properties properties = new Properties();
        properties.put("serverAddr", nacosProperties.getServerAddr());
        String namespace = nacosProperties.getNamespace();
        if (namespace == null || namespace.trim().isEmpty()) {
            properties.put("namespace", namespace);
        }
        ConfigService service = ConfigFactory.createConfigService(properties);
        return service;
    }

}
