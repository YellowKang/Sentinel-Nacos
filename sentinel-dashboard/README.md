# 直接Clone项目

​		启动参数，配置完成后进行数据新增，自动生成配置文件到Nacos以及ID生成策略和持久化

```properties
# Nacos地址
nacos.server.ip=127.0.0.1
# Nacos命名空间（建议sentinel）
nacos.server.namespace=sentinel
# Nacos端口号
nacos.server.port=8848
# GROUP_ID自定义
nacos.server.groupId=DEFAULT_GROUP
# 是否开启Nacos存储
nacos.server.enable=true
```

​		打包即可，本次修改源码版本为1.8.1

​		修改类如下

```properties
# 新增
NacosProperties
NacosConfig
RuleTypeEnum
NacosUtil

# 修改

InMemAuthorityRuleStore
InMemDegradeRuleStore
InMemFlowRuleStore
InMemoryRuleRepositoryAdapter
InMemParamFlowRuleStore
InMemSystemRuleStore
AppController
AuthController
AuthorityRuleController
DegradeController
DemoController
FlowControllerV1
MachineRegistryController
MetricController
ParamFlowRuleController
ResourceController
SystemController
VersionController
FlowControllerV2
```

