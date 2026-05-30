-- =============================================
-- 系统百科平台 - 演示数据
-- 执行方式: mysql -uroot -proot syswiki < demo-data.sql
-- =============================================

USE syswiki;

-- 清除旧演示数据
DELETE FROM sys_ency_content_version WHERE system_id IN ('SP20260530000001', 'SP20260530000002');
DELETE FROM sys_ency_sql_lib WHERE system_id IN ('SP20260530000001', 'SP20260530000002');
DELETE FROM sys_ency_topology WHERE system_id IN ('SP20260530000001', 'SP20260530000002');
DELETE FROM sys_ency_content WHERE system_id IN ('SP20260530000001', 'SP20260530000002');
DELETE FROM sys_ency_space WHERE system_id IN ('SP20260530000001', 'SP20260530000002');

-- =============================================
-- 1. 系统空间
-- =============================================
INSERT IGNORE INTO sys_ency_space (system_id, system_name, system_code, owner, description, status, create_time)
VALUES
('SP20260530000001', '超级网上银行系统', 'SIB', '张三', '超级网上银行核心渠道系统，承载个人网银、企业网银、手机银行等全渠道业务', 'ACTIVE', NOW()),
('SP20260530000002', '跨境支付系统', 'CPS', '李四', '跨境支付核心处理系统，对接SWIFT、CIPS等国际清算通道', 'ACTIVE', NOW());

-- =============================================
-- 2. 百科内容 - 系统简介与技术栈 (INTRO)
-- =============================================
INSERT INTO sys_ency_content (content_id, system_id, module_type, md_content, version, operator, create_time)
VALUES
('CT20260530000001', 'SP20260530000001', 'INTRO',
'# 超级网上银行系统简介

超级网上银行系统（SIB）是我行核心渠道系统，承载个人网银、企业网银、手机银行、微信银行等全渠道业务入口。系统日均交易量超过500万笔，服务客户超过3000万。

## 核心功能

- 个人网银：账户查询、转账汇款、理财购买、信用卡管理
- 企业网银：批量代发、资金归集、票据管理、银企对账
- 手机银行：移动转账、扫码支付、生活缴费、贷款申请
- 开放银行：API网关、SDK对接、第三方渠道接入

## 技术栈

- Java 11
- Spring Boot 2.7
- Spring Cloud Gateway
- MyBatis-Plus
- Redis 7.0
- RocketMQ 5.0
- OceanBase 4.0
- Nacos 2.2
- Sentinel 1.8
- Vue 3
- Element Plus
- Nginx',
1, '张三', NOW()),

-- 测试环境架构
('CT20260530000002', 'SP20260530000001', 'ARCH_TEST',
'# 测试环境架构

## 物理架构

测试环境采用3节点应用集群 + 1组数据库主备架构。

| 节点 | IP | 角色 |
|------|-----|------|
| APP-01 | 10.1.10.11 | 应用节点1（网关+业务服务） |
| APP-02 | 10.1.10.12 | 应用节点2（网关+业务服务） |
| APP-03 | 10.1.10.13 | 应用节点3（定时任务+报表） |
| DB-Master | 10.1.10.21 | OceanBase主节点 |
| DB-Slave | 10.1.10.22 | OceanBase备节点 |
| Redis-01 | 10.1.10.31 | Redis主节点 |
| MQ-01 | 10.1.10.41 | RocketMQ NameServer |

## 逻辑架构

```
用户请求 → Nginx负载均衡 → Spring Cloud Gateway → 业务微服务集群
                                                    ↓
                              Redis缓存 ← 业务服务 → OceanBase数据库
                                                    ↓
                                              RocketMQ消息队列
```

## 信创架构说明

测试环境已全面适配信创要求：
- 操作系统：银河麒麟V10
- 数据库：OceanBase 4.0（MySQL兼容模式）
- 中间件：东方通TongWeb替代Tomcat',
1, '张三', NOW()),

-- 生产环境架构
('CT20260530000003', 'SP20260530000001', 'ARCH_PROD',
'# 生产环境架构

## 物理架构

生产环境采用双中心部署，主机房（北京）+ 同城灾备机房（亦庄）。

### 主机房（北京）

| 节点 | IP | 角色 | 配置 |
|------|-----|------|------|
| PROD-APP-01~06 | 10.2.1.11~16 | 应用节点 | 16C32G |
| PROD-GW-01~02 | 10.2.1.21~22 | 网关节点 | 8C16G |
| PROD-DB-01~03 | 10.2.1.31~33 | OB三副本集群 | 32C128G |
| PROD-Redis-01~03 | 10.2.1.41~43 | Redis哨兵集群 | 8C16G |
| PROD-MQ-01~02 | 10.2.1.51~52 | RocketMQ集群 | 8C16G |

### 灾备机房（亦庄）

| 节点 | IP | 角色 |
|------|-----|------|
| DR-APP-01~03 | 10.2.2.11~13 | 应用节点（热备） |
| DR-DB-01~03 | 10.2.2.31~33 | OB只读副本 |

## 高可用设计

- 应用层：6节点集群，单节点故障自动摘除
- 数据库：OceanBase三副本，RPO=0，RTO<30s
- 缓存：Redis哨兵模式，自动主从切换
- 消息：RocketMQ双主双从，消息零丢失',
2, '张三', NOW()),

-- 服务器配置
('CT20260530000004', 'SP20260530000001', 'SERVER',
'# 服务器配置清单

## 应用服务器

| IP | 内网域名 | 部署模块 | CPU | 内存 | 磁盘 |
|----|---------|---------|-----|------|------|
| 10.2.1.11 | sib-app-01.internal | 网关+用户服务 | 16C | 32G | 200G SSD |
| 10.2.1.12 | sib-app-02.internal | 网关+用户服务 | 16C | 32G | 200G SSD |
| 10.2.1.13 | sib-app-03.internal | 交易服务 | 16C | 32G | 200G SSD |
| 10.2.1.14 | sib-app-04.internal | 交易服务 | 16C | 32G | 200G SSD |
| 10.2.1.15 | sib-app-05.internal | 理财+报表服务 | 16C | 32G | 500G SSD |
| 10.2.1.16 | sib-app-06.internal | 定时任务+对账 | 16C | 32G | 500G SSD |

## 数据库服务器

| IP | 内网域名 | 角色 | CPU | 内存 | 磁盘 |
|----|---------|------|-----|------|------|
| 10.2.1.31 | sib-db-01.internal | OB Zone1 | 32C | 128G | 2T SSD |
| 10.2.1.32 | sib-db-02.internal | OB Zone2 | 32C | 128G | 2T SSD |
| 10.2.1.33 | sib-db-03.internal | OB Zone3 | 32C | 128G | 2T SSD |

## 中间件服务器

| IP | 内网域名 | 服务 | CPU | 内存 |
|----|---------|------|-----|------|
| 10.2.1.41 | sib-redis-01.internal | Redis Sentinel | 8C | 16G |
| 10.2.1.42 | sib-redis-02.internal | Redis Slave | 8C | 16G |
| 10.2.1.43 | sib-redis-03.internal | Redis Slave | 8C | 16G |
| 10.2.1.51 | sib-mq-01.internal | RocketMQ Master | 8C | 16G |
| 10.2.1.52 | sib-mq-02.internal | RocketMQ Slave | 8C | 16G |',
1, '张三', NOW()),

-- 网络策略
('CT20260530000005', 'SP20260530000001', 'NETWORK',
'# 网络策略管理

## 防火墙策略

| 源地址 | 目标地址 | 端口 | 协议 | 状态 |
|--------|---------|------|------|------|
| 10.1.0.0/16 | 10.2.1.11~16 | 8080 | TCP | 已开通 |
| 10.2.1.11~16 | 10.2.1.31~33 | 2881 | TCP | 已开通 |
| 10.2.1.11~16 | 10.2.1.41~43 | 6379 | TCP | 已开通 |
| 10.2.1.11~16 | 10.2.1.51~52 | 10911 | TCP | 已开通 |
| 10.2.1.0/24 | 10.3.1.0/24 | 443 | TCP | 申请中 |
| 外部CDN | 10.2.1.21~22 | 443 | HTTPS | 已开通 |

## F5负载均衡配置

| VIP | 后端池 | 权重 | 监听端口 | 健康检查 |
|-----|--------|------|---------|---------|
| 10.2.1.100 | sib-gw-pool | 1:1:1 | 443 | HTTPS /health |
| 10.2.1.101 | sib-app-pool | 1:1:1:1:1:1 | 8080 | HTTP /actuator/health |

### sib-gw-pool 成员
- 10.2.1.21:443 权重1
- 10.2.1.22:443 权重1

### sib-app-pool 成员
- 10.2.1.11:8080 权重1
- 10.2.1.12:8080 权重1
- 10.2.1.13:8080 权重1
- 10.2.1.14:8080 权重1
- 10.2.1.15:8080 权重1
- 10.2.1.16:8080 权重1

## 域名映射

| 域名 | 映射目标 | 用途 |
|------|---------|------|
| ibank.bank.com | 10.2.1.100:443 | 个人网银 |
| ebank.bank.com | 10.2.1.100:443 | 企业网银 |
| api.bank.com | 10.2.1.100:443 | 开放银行API |
| sib-admin.internal | 10.2.1.101:8080 | 管理后台 |',
1, '张三', NOW()),

-- 数据库配置
('CT20260530000006', 'SP20260530000001', 'DATABASE',
'# 数据库配置

## OceanBase集群信息

| 属性 | 值 |
|------|-----|
| 集群名称 | sib-ob-cluster |
| 版本 | OceanBase 4.0.0.0 |
| 部署模式 | 三机房三副本 |
| 租户名称 | sib_tenant |
| 字符集 | utf8mb4 |
| 比较规则 | utf8mb4_general_ci |

## 主备节点状态

| 节点 | IP | Zone | 角色 | 状态 |
|------|-----|------|------|------|
| OB-Node1 | 10.2.1.31 | zone1 | Leader | 正常 |
| OB-Node2 | 10.2.1.32 | zone2 | Follower | 正常 |
| OB-Node3 | 10.2.1.33 | zone3 | Follower | 正常 |

## 连接池配置

```
# HikariCP 连接池
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

## 读写分离策略

- 写操作：路由至Leader节点（zone1）
- 读操作：通过Follower节点负载均衡（zone2 + zone3）
- 强一致读：通过Hint指定读Leader

## 核心库表清单

| 表名 | 数据量 | 说明 |
|------|--------|------|
| t_user_info | 3200万 | 用户信息表 |
| t_account | 4500万 | 账户表 |
| t_trans_log | 8.5亿 | 交易流水表（按月分表） |
| t_order | 2.1亿 | 订单表（按季度分表） |
| t_card_info | 1800万 | 银行卡信息表 |',
1, '张三', NOW()),

-- 快速接入指南
('CT20260530000007', 'SP20260530000001', 'GUIDE',
'# 快速接入指南

## 接入流程

1. **申请接入**：在工单系统提交「渠道接入申请」，填写接入系统名称、负责人、预计QPS
2. **环境准备**：获取测试环境账号，配置网络白名单
3. **SDK集成**：引入Maven依赖，配置连接参数
4. **联调测试**：使用测试环境沙箱账号完成联调
5. **安全评审**：提交安全评审申请，通过后获取生产环境密钥
6. **上线发布**：配合灰度发布流程，逐步放量

## Maven依赖

```xml
<!-- 网银开放平台SDK -->
<dependency>
    <groupId>com.bank.sib</groupId>
    <artifactId>sib-open-sdk</artifactId>
    <version>2.7.3</version>
</dependency>

<!-- 加密工具包 -->
<dependency>
    <groupId>com.bank.sib</groupId>
    <artifactId>sib-crypto</artifactId>
    <version>1.2.0</version>
</dependency>
```

## 测试环境配置

```
sib.api.base-url=https://sib-test.internal.bank.com/api
sib.api.app-id=YOUR_APP_ID
sib.api.app-secret=YOUR_APP_SECRET
sib.api.sign-type=RSA2
sib.api.connect-timeout=5000
sib.api.read-timeout=30000
```

## 联调注意事项

- 测试环境交易不会实际扣款，使用沙箱账户
- 测试环境每日22:00-次日06:00可能有定时维护
- 联调前请先在工单系统预约联调窗口
- 报文签名使用RSA2算法，公钥需提前上传至开放平台',
1, '张三', NOW());

-- =============================================
-- 3. 拓扑链路配置
-- =============================================
INSERT INTO sys_ency_topology (link_id, system_id, from_node, to_node, protocol, interface_name, interface_details, sort_order, create_time)
VALUES
('TL20260530000001', 'SP20260530000001', '手机银行APP', 'Nginx负载均衡', 'HTTPS', '/api/v1/mobile/*', '### 移动端接入\n- 协议: HTTPS/TLS 1.3\n- 认证: Token + 设备指纹\n- 限流: 1000 QPS/设备', 1, NOW()),
('TL20260530000002', 'SP20260530000001', 'Nginx负载均衡', 'API网关', 'HTTP', ':8080', '### 网关路由\n- 路由规则: 按path前缀分发\n- 限流: Sentinel集群限流\n- 熔断: 慢调用比例 > 50% 触发', 2, NOW()),
('TL20260530000003', 'SP20260530000001', 'API网关', '用户服务', 'RPC', 'UserService/getUserInfo', '### 用户查询接口\n| 字段 | 类型 | 说明 |\n|------|------|------|\n| userId | string | 用户编号 |\n| userName | string | 用户姓名 |\n| accountList | array | 关联账户列表 |', 3, NOW()),
('TL20260530000004', 'SP20260530000001', 'API网关', '交易服务', 'RPC', 'TransService/transfer', '### 转账交易接口\n| 字段 | 类型 | 说明 |\n|------|------|------|\n| fromAcct | string | 付款账号 |\n| toAcct | string | 收款账号 |\n| amount | decimal | 转账金额 |\n| memo | string | 摘要 |', 4, NOW()),
('TL20260530000005', 'SP20260530000001', '交易服务', '核心记账引擎', 'TCP/ISO8583', 'TRANS_001', '### ISO8583报文\n- 域2: 主账号(PAN)\n- 域3: 处理码(000000=消费)\n- 域4: 交易金额\n- 域7: 交易时间\n- 域11: 流水号\n- 域41: 终端号', 5, NOW()),
('TL20260530000006', 'SP20260530000001', '交易服务', 'OceanBase', 'JDBC', ':2881', '### 数据库访问\n- 连接池: HikariCP, max=50\n- 读写分离: 写Leader, 读Follower\n- 慢SQL阈值: 500ms', 6, NOW()),
('TL20260530000007', 'SP20260530000001', '交易服务', 'Redis集群', 'TCP', ':6379', '### 缓存用途\n- 用户Session: TTL=30min\n- 交易限流: 滑动窗口\n- 热点数据: 账户余额缓存', 7, NOW()),
('TL20260530000008', 'SP20260530000001', '交易服务', 'RocketMQ', 'TCP', ':10911', '### 消息主题\n- TRANS_NOTIFY: 交易结果通知\n- TRANS_ASYNC: 异步记账\n- TRANS_LOG: 交易日志归档', 8, NOW());

-- =============================================
-- 4. 运维SQL库
-- =============================================
INSERT INTO sys_ency_sql_lib (sql_id, system_id, title, category, sql_template, description, params_json, sort_order, operator, create_time)
VALUES
('SQ20260530000001', 'SP20260530000001', '根据交易单号查询流水', 'QUERY',
'SELECT trans_no, from_acct, to_acct, amount, status, trans_time FROM t_trans_log WHERE bill_no = :billNo AND trans_date = :transDate',
'根据交易单号和日期查询交易流水记录',
'[{"name":"billNo","label":"交易单号","placeholder":"如: TX20260528001"},{"name":"transDate","label":"交易日期","placeholder":"如: 20260528"}]',
1, '张三', NOW()),

('SQ20260530000002', 'SP20260530000001', '查询用户关联账户', 'QUERY',
'SELECT acct_no, acct_type, balance, status, open_date FROM t_account WHERE user_id = :userId AND status = ''A'' ORDER BY open_date DESC',
'根据用户编号查询所有有效账户',
'[{"name":"userId","label":"用户编号","placeholder":"如: U100001"}]',
2, '张三', NOW()),

('SQ20260530000003', 'SP20260530000001', '查询今日交易汇总', 'QUERY',
'SELECT trans_type, COUNT(*) as cnt, SUM(amount) as total FROM t_trans_log WHERE trans_date = DATE_FORMAT(NOW(), ''%Y%m%d'') GROUP BY trans_type ORDER BY total DESC',
'查询当日各交易类型的笔数和金额汇总，无需参数',
'[]',
3, '张三', NOW()),

('SQ20260530000004', 'SP20260530000001', '核对账户余额', 'CHECK',
'SELECT a.acct_no, a.balance as acct_balance, SUM(CASE WHEN t.trans_type = ''IN'' THEN t.amount ELSE -t.amount END) as calc_balance, a.balance - SUM(CASE WHEN t.trans_type = ''IN'' THEN t.amount ELSE -t.amount END) as diff FROM t_account a LEFT JOIN t_trans_log t ON a.acct_no = t.from_acct OR a.acct_no = t.to_acct WHERE a.acct_no = :acctNo GROUP BY a.acct_no, a.balance',
'校验账户余额与交易流水是否一致',
'[{"name":"acctNo","label":"账号","placeholder":"如: 6228480012345678"}]',
4, '张三', NOW()),

('SQ20260530000005', 'SP20260530000001', '冲正失败交易', 'FIX',
'UPDATE t_trans_log SET status = ''REVERSED'', update_time = NOW() WHERE bill_no = :billNo AND status = ''FAILED'' AND trans_date = :transDate',
'将失败交易标记为已冲正状态（仅限状态为FAILED的交易）',
'[{"name":"billNo","label":"交易单号"},{"name":"transDate","label":"交易日期","placeholder":"如: 20260528"}]',
5, '张三', NOW()),

('SQ20260530000006', 'SP20260530000001', '慢SQL查询TOP10', 'PERF',
'SELECT SQL_TEXT, AVG_TIMER_WAIT/1000000 as avg_ms, COUNT_STAR as exec_count FROM performance_schema.events_statements_summary_by_digest WHERE SCHEMA_NAME = ''syswiki'' ORDER BY AVG_TIMER_WAIT DESC LIMIT 10',
'查询执行时间最长的TOP10慢SQL（需performance_schema开启）',
'[]',
6, '张三', NOW()),

('SQ20260530000007', 'SP20260530000001', '查看连接池使用情况', 'PERF',
'SHOW STATUS WHERE Variable_name IN (''Threads_connected'', ''Threads_running'', ''Max_used_connections'', ''Aborted_connects'')',
'查看当前数据库连接池使用状态，无需参数',
'[]',
7, '张三', NOW()),

('SQ20260530000008', 'SP20260530000001', 'grep应用日志ERROR', 'SHELL',
'grep -n "ERROR" /data/logs/sib-app/application.log | tail -50',
'查看应用日志最近50条ERROR记录',
'[]',
8, '张三', NOW()),

('SQ20260530000009', 'SP20260530000001', '查看GC日志', 'SHELL',
'tail -100 /data/logs/sib-app/gc.log | grep -E "Full GC|Allocation Failure"',
'查看最近100行GC日志中的Full GC记录，排查内存问题',
'[]',
9, '张三', NOW()),

('SQ20260530000010', 'SP20260530000001', '重启应用服务', 'SHELL',
'systemctl restart sib-app && sleep 5 && systemctl status sib-app',
'重启应用服务并检查状态（需root权限）',
'[]',
10, '张三', NOW());
