package com.alibaba.csp.sentinel.dashboard.repository.rule;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.nacos.NacosUtil;
import com.alibaba.csp.sentinel.dashboard.nacos.RuleTypeEnum;
import com.alibaba.csp.sentinel.util.AssertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author leyou
 */
public abstract class InMemoryRuleRepositoryAdapter<T extends RuleEntity> implements RuleRepository<T, Long> {
    private static Logger logger = LoggerFactory.getLogger(NacosUtil.class);

    /**
     * {@code <machine, <id, rule>>}
     */
    private Map<MachineInfo, Map<Long, T>> machineRules = new ConcurrentHashMap<>(16);
    private Map<Long, T> allRules = new ConcurrentHashMap<>(16);

    private Map<String, Map<Long, T>> appRules = new ConcurrentHashMap<>(16);

    private static final int MAX_RULES_SIZE = 10000;

    // 获取当前T的class
    ParameterizedType type = (ParameterizedType) this.getClass().getGenericSuperclass();
    Class<T> tClass = (Class) type.getActualTypeArguments()[0];

    /**
     * Nacos工具类子类通过构造
     */
    private NacosUtil nacosUtil;

    /**
     * 路由类型枚举
     */
    private RuleTypeEnum ruleTypeEnum;

    private static AtomicLong CUSTOM_ID = new AtomicLong(0);

    public InMemoryRuleRepositoryAdapter(NacosUtil nacosUtil, RuleTypeEnum ruleTypeEnum) {
        this.nacosUtil = nacosUtil;
        this.ruleTypeEnum = ruleTypeEnum;
    }

    /**
     * 更新数据到Nacos中，并且推送到服务中
     *
     * @param appName
     * @param
     */
    public void updateAppByNacos(String appName) {
        if (nacosUtil.isOpen()) {
            // 查询出新增的数据对应的APP的所有数据，存储到Nacos
            List<T> allByApp = findAllByApp(appName);
            nacosUtil.publisNacos(appName, ruleTypeEnum, allByApp);
            nacosUtil.publishRules(appName, ruleTypeEnum, allByApp);
        }

    }

    public T saveAll(T entity, boolean saveAll) {
        // 启用Nacos则进行NacosID生成，没有启用则使用默认
        if (entity.getId() == null) {
            if (nacosUtil.isOpen()) {
                entity.setId(nacosUtil.nextId(this.ruleTypeEnum, saveAll));
            } else {
                entity.setId(nextId());
            }
        }
        T processedEntity = preProcess(entity);
        if (processedEntity != null) {
            allRules.put(processedEntity.getId(), processedEntity);
            machineRules.computeIfAbsent(MachineInfo.of(processedEntity.getApp(), processedEntity.getIp(),
                    processedEntity.getPort()), e -> new ConcurrentHashMap<>(32))
                    .put(processedEntity.getId(), processedEntity);
            appRules.computeIfAbsent(processedEntity.getApp(), v -> new ConcurrentHashMap<>(32))
                    .put(processedEntity.getId(), processedEntity);
            // 同步数据到Nacos
            updateAppByNacos(entity.getApp());
        }

        return processedEntity;
    }

    @Override
    public T save(T entity) {
        return saveAll(entity, false);
    }

    @Override
    public List<T> saveAll(List<T> rules) {
        // TODO: check here.
        allRules.clear();
        machineRules.clear();
        appRules.clear();

        if (rules == null) {
            return null;
        }
        List<T> savedRules = new ArrayList<>(rules.size());
        for (T rule : rules) {
            savedRules.add(saveAll(rule, true));
        }
        if(nacosUtil.isOpen()){
            nacosUtil.syncNextId(this.ruleTypeEnum);
        }
        return savedRules;
    }

    @Override
    public T delete(Long id) {
        T entity = allRules.remove(id);
        if (entity != null) {
            if (appRules.get(entity.getApp()) != null) {
                appRules.get(entity.getApp()).remove(id);
            }
            machineRules.get(MachineInfo.of(entity.getApp(), entity.getIp(), entity.getPort())).remove(id);
            // 同步数据到Nacos
            updateAppByNacos(entity.getApp());
        }
        return entity;
    }

    @Override
    public T findById(Long id) {
        return allRules.get(id);
    }

    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        Map<Long, T> entities = machineRules.get(machineInfo);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    @Override
    public List<T> findAllByApp(String appName) {
        AssertUtil.notEmpty(appName, "appName cannot be empty");
        Map<Long, T> entities = appRules.get(appName);
        List<T> listData = new ArrayList<>();
        if (entities == null) {
            // 查询APP数据为空的时候就先从Nacos查询好，然后进行写入
            if (nacosUtil.isOpen()) {
                try {
                    List<T> rules = nacosUtil.getRules(appName, this.ruleTypeEnum, tClass);
                    if (rules != null && rules.size() > 0) {
                        for (T rule : rules) {
                            listData.add(rule);
                            save(rule);
                        }
                    }
                } catch (Exception e) {
                    logger.error("从Nacos拉取查询数据失败,AppName:{},类型:{},数据:{}", appName, this.ruleTypeEnum.name());
                    e.printStackTrace();
                }
            }
        } else {
            listData = new ArrayList<>(entities.values());
        }
        return listData;
    }

    public void clearAll() {
        allRules.clear();
        machineRules.clear();
        appRules.clear();
    }

    protected T preProcess(T entity) {
        return entity;
    }

    /**
     * Get next unused id.
     *
     * @return next unused id
     */
    abstract protected long nextId();
}
