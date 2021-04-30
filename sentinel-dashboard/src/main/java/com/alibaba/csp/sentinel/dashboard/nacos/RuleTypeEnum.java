package com.alibaba.csp.sentinel.dashboard.nacos;


/**
 * @Author HuangKang
 * @Date 2021/4/29 下午5:17
 * @Summarize 路由类型枚举
 * 将新增的APP的规则，如果 ${spring.application.name}-flow-rules,文件写入Nacos，其他类型依旧如此
 */
public enum RuleTypeEnum {

    /**
     * 限流流控
     */
    FLOW("-flow-rules"),
    /**
     * 服务降级熔断
     */
    DEGRADE("-degrade-rules"),
    /**
     * 参数流控限流
     */
    PARAM("-param-rules"),
    /**
     * 认证规则
     */
    AUTHORITY("-authority-rules"),
    /**
     * 系统路由
     */
    SYSTEM("-system-rules"),

    /**
     * API接口路由
     */
    API("-api-rules"),

    /**
     * 网关路由
     */
    GATEWAY("-gateway-rules");

    RuleTypeEnum(String suffix) {
        this.suffix = suffix;
    }

    private String suffix;

    public String getSuffix() {
        return suffix;
    }
}
