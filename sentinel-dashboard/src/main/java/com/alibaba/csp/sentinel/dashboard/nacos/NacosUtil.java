package com.alibaba.csp.sentinel.dashboard.nacos;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author HuangKang
 * @Date 2021/4/29 下午6:00
 * @Summarize Nacos工具类用于获取数据，以及
 */
@Component
public class NacosUtil {
    private static Logger logger = LoggerFactory.getLogger(NacosUtil.class);

    private final NacosProperties nacosProperties;

    @Autowired(required = false)
    private ConfigService configService;

    @Autowired
    private SentinelApiClient sentinelApiClient;

    @Autowired
    private AppManagement appManagement;

    /**
     * NEXT下一个ID缓存
     */
    private static final ConcurrentHashMap<String, Long> NEXT_ID_CACHE = new ConcurrentHashMap<>();

    @Autowired
    public NacosUtil(NacosProperties nacosProperties) {
        this.nacosProperties = nacosProperties;
    }

    /**
     * 推送数据到服务节点中
     *
     * @param appName  SpringCloud注册的项目名
     * @param ruleType 路由类型
     * @param listData
     * @param <T>
     */
    public <T> void publishRules(String appName, RuleTypeEnum ruleType, List listData) {
        if (StringUtil.isBlank(appName)) {
            return;
        }
        if (appName == null) {
            return;
        }
        Set<MachineInfo> set = appManagement.getDetailApp(appName).getMachines();
        logger.info("推送AppName:{},类型:{}规则到:{}个服务中", appName, ruleType.name(), set.size());
        for (MachineInfo machine : set) {
            if (!machine.isHealthy()) {
                continue;
            }
            // 根据不同类型数据推送到节点中
            if (RuleTypeEnum.FLOW.equals(ruleType)) {
                sentinelApiClient.setFlowRuleOfMachine(appName, machine.getIp(), machine.getPort(), listData);
            } else if (RuleTypeEnum.DEGRADE.equals(ruleType)) {
                sentinelApiClient.setDegradeRuleOfMachine(appName, machine.getIp(), machine.getPort(), listData);
            } else if (RuleTypeEnum.PARAM.equals(ruleType)) {
                sentinelApiClient.setParamFlowRuleOfMachine(appName, machine.getIp(), machine.getPort(), listData);
            } else if (RuleTypeEnum.AUTHORITY.equals(ruleType)) {
                sentinelApiClient.setAuthorityRuleOfMachine(appName, machine.getIp(), machine.getPort(), listData);
            } else if (RuleTypeEnum.SYSTEM.equals(ruleType)) {
                sentinelApiClient.setSystemRuleOfMachine(appName, machine.getIp(), machine.getPort(), listData);
            }
        }
    }


    /**
     * 推送本地路由数据到Nacos
     *
     * @param appName  SpringCloud注册的项目名
     * @param ruleType 路由类型
     * @param listData
     * @param <T>
     */
    public <T> void publisNacos(String appName, RuleTypeEnum ruleType, List<T> listData) {
        try {
            logger.info("推送数据到Nacos,AppName:{},类型:{},数据:{}", appName, ruleType.name(), JSON.toJSONString(listData));
            configService.publishConfig(appName + ruleType.getSuffix(),
                    nacosProperties.getGroupId(), encoder(listData));
        } catch (NacosException e) {
            logger.error("ERROR--推送Nacos同步数据失败!!!AppName:{},类型:{},数据:{}", appName, ruleType.name(), JSON.toJSONString(listData));
        }
    }

    /**
     * 从Nacos查询数据
     * 根据APP名称，加上路由类型，以及返回数据集合，从Nacos获取数据并且返回对象集合
     *
     * @param appName  SpringCloud注册的项目名
     * @param ruleType 路由类型
     * @param classAs  实体类class
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> List<T> getRules(String appName, RuleTypeEnum ruleType, Class<T> classAs) throws Exception {
        // 从Nacos拉取数据，项目名+类型后缀
        String rules = configService.getConfig(appName + ruleType.getSuffix(),
                nacosProperties.getGroupId(), 3000);
        logger.info("从Nacos拉取查询数据,AppName:{},类型:{},数据:{}", appName, ruleType.name(), rules);
        if (StringUtil.isEmpty(rules)) {
            return new ArrayList<>();
        }
        return decoder(rules, classAs);
    }

    /**
     * 数据序列化，将对象集合序列化为Json字符串
     *
     * @param listData 原数据
     * @param <T>
     * @return
     */
    public <T> String encoder(List<T> listData) {
        return JSON.toJSONString(listData);
    }

    /**
     * 数据序反列化，将Json字符串反序列化为对象集合
     *
     * @param jsonData Json字符串，从Nacos拉取
     * @param classAs  返回的对象类型
     * @param <T>
     * @return
     */
    public <T> List<T> decoder(String jsonData, Class<T> classAs) {
        return JSON.parseArray(jsonData, classAs);
    }

    /**
     * 是否开启Nacos
     *
     * @return
     */
    public boolean isOpen() {
        return nacosProperties.isOpen();
    }


    /**
     * 同步本地NextID到本地
     *
     * @param ruleType
     */
    public void syncNextId(RuleTypeEnum ruleType) {
        // 从缓存中获取nextId
        Long nextId = NEXT_ID_CACHE.get(ruleType.name());
        try {
            if (nextId == null) {
                nextId = nextId(ruleType, false);
            }
            configService.publishConfig("a-custom" + ruleType.getSuffix() + "-next-id",
                    nacosProperties.getGroupId(), new Long(nextId).toString());
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    /**
     * ID策略存储Nacos
     *
     * @return
     */
    public Long nextId(RuleTypeEnum ruleType, boolean saveAll) {
        synchronized (ruleType) {
            try {
                // 从缓存中获取nextId
                Long nextId = NEXT_ID_CACHE.get(ruleType.name());
                if (nextId == null) {
                    String nextIdStr = configService.getConfig("a-custom" + ruleType.getSuffix() + "-next-id",
                            nacosProperties.getGroupId(), 3000);
                    // 如果Nacos中也没有，则初始化
                    if (nextIdStr == null || nextIdStr.trim().isEmpty()) {
                        nextId = 1L;
                    }
                    // 如果有则进行序列化
                    else {
                        nextId = Long.valueOf(nextIdStr);
                    }
                }
                nextId += 1;
                NEXT_ID_CACHE.put(ruleType.name(), nextId);
                // 批量添加时不全局添加ID
                if (!saveAll) {
                    // 更新NacosID
                    configService.publishConfig("a-custom" + ruleType.getSuffix() + "-next-id",
                            nacosProperties.getGroupId(), new Long(nextId).toString());
                }
                return nextId;
            } catch (NacosException e) {
                logger.error("生成Nacos失败ID!!!");
                e.printStackTrace();
            }
        }
        return null;
    }
}
