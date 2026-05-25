# 数据库连接池优化指南

## 当前配置分析

### 现有配置 (Druid)
```yaml
spring:
  datasource:
    druid:
      initial-size: 3           # 初始连接数
      min-idle: 3               # 最小空闲连接
      max-active: 10            # 最大活跃连接
      max-wait: 60000           # 获取连接最大等待时间(毫秒)
```

### 存在的问题

1. **连接数偏小**: max-active=10 对于高并发场景可能不足
2. **缺少监控**: 没有启用详细的性能监控
3. **缺少泄漏检测**: 没有配置连接泄漏检测
4. **慢SQL阈值**: slowSqlMillis=5000 可能过大

---

## 优化方案

### 方案一：优化 Druid 配置（推荐）✅

#### 优化后的配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/vehicle_fence?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=30000&socketTimeout=30000&rewriteBatchedStatements=true
    username: root
    password: wzjwzj11
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      # ========== 连接池大小配置 ==========
      initial-size: 5                    # 初始连接数（根据启动时负载调整）
      min-idle: 5                        # 最小空闲连接
      max-active: 20                     # 最大活跃连接（根据并发量调整）
      
      # ========== 连接超时配置 ==========
      max-wait: 30000                    # 获取连接最大等待时间(毫秒)，降低到30秒
      
      # ========== 连接保活配置 ==========
      time-between-eviction-runs-millis: 60000        # 检测间隔(毫秒)
      min-evictable-idle-time-millis: 300000          # 最小空闲时间(毫秒)
      max-evictable-idle-time-millis: 900000          # 最大空闲时间(毫秒)
      
      # ========== 连接验证配置 ==========
      validation-query: SELECT 1                      # 验证查询
      test-while-idle: true                           # 空闲时验证
      test-on-borrow: false                           # 借出时不验证（影响性能）
      test-on-return: false                           # 归还时不验证
      
      # ========== 预编译语句缓存 ==========
      pool-prepared-statements: true                  # 开启PSCache
      max-pool-prepared-statement-per-connection-size: 20
      
      # ========== 连接泄漏检测 ==========
      remove-abandoned: true                          # 开启泄漏检测
      remove-abandoned-timeout: 180                   # 泄漏超时时间(秒)
      log-abandoned: true                             # 记录泄漏日志
      
      # ========== 过滤器配置 ==========
      filters: stat,wall,slf4j                        # 添加slf4j日志过滤
      filter:
        stat:
          enabled: true
          db-type: mysql
          log-slow-sql: true                          # 记录慢SQL
          slow-sql-millis: 2000                       # 慢SQL阈值(毫秒)，降低到2秒
          merge-sql: true                             # 合并相同SQL统计
        wall:
          enabled: true
          config:
            drop-table-allow: false                   # 禁止删表操作
            multi-statement-allow: false              # 禁止批量执行
      
      # ========== 监控配置 ==========
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
        session-stat-enable: true
        session-stat-max-count: 1000
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin                         # 修改默认密码！
        login-password: admin@123
        allow: 127.0.0.1                              # 只允许本地访问
        reset-enable: false                           # 禁止重置统计
```

#### 关键优化点说明

**1. 连接池大小调整**
- `initial-size`: 3 → 5 （提升启动性能）
- `min-idle`: 3 → 5 （保持足够的空闲连接）
- `max-active`: 10 → 20 （支持更高并发）

**计算公式：**
```
max-active = (QPS * 平均响应时间) + 缓冲
例如：QPS=100, 平均RT=50ms → max-active = 100 * 0.05 + 5 = 10

对于本项目（IoT车辆追踪）：
- 假设100辆车，每10秒上报一次 → QPS = 10
- 加上查询请求，预估峰值QPS = 50
- 建议 max-active = 20-30
```

**2. 连接泄漏检测**
```yaml
remove-abandoned: true              # 自动回收泄漏的连接
remove-abandoned-timeout: 180       # 3分钟未归还视为泄漏
log-abandoned: true                 # 记录泄漏堆栈信息
```

**3. 慢SQL监控**
```yaml
slow-sql-millis: 2000               # 从5秒降低到2秒
log-slow-sql: true                  # 记录慢SQL到日志
```

**4. JDBC URL 优化**
```
&rewriteBatchedStatements=true      # 开启批量写入优化
```

---

### 方案二：切换到 HikariCP（备选）

如果考虑更轻量级的连接池，可以切换到 HikariCP（Spring Boot 默认）。

#### 配置示例

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/vehicle_fence?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=30000&socketTimeout=30000&rewriteBatchedStatements=true
    username: root
    password: wzjwzj11
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 移除 type 配置，使用默认的 HikariCP
    hikari:
      # ========== 连接池大小 ==========
      minimum-idle: 5                # 最小空闲连接
      maximum-pool-size: 20          # 最大连接池大小
      
      # ========== 超时配置 ==========
      connection-timeout: 30000      # 连接超时(毫秒)
      idle-timeout: 600000           # 空闲超时(毫秒) = 10分钟
      max-lifetime: 1800000          # 最大生命周期(毫秒) = 30分钟
      
      # ========== 连接测试 ==========
      connection-test-query: SELECT 1  # 连接测试查询
      
      # ========== 性能优化 ==========
      auto-commit: true              # 自动提交
      pool-name: VehicleFenceHikariCP
      register-mbeans: true          # 注册MBeans用于监控
      
      # ========== Leak 检测 ==========
      leak-detection-threshold: 60000  # 泄漏检测阈值(毫秒)
```

#### Druid vs HikariCP 对比

| 特性 | Druid | HikariCP |
|------|-------|----------|
| 性能 | 良好 | 优秀（更快） |
| 功能 | 丰富（监控、防火墙） | 简洁 |
| 内存占用 | 较高 | 较低 |
| 学习成本 | 中等 | 低 |
| 适用场景 | 需要详细监控 | 追求高性能 |

**建议**: 本项目继续使用 Druid，因为已经集成了监控和SQL防火墙功能。

---

## 连接池大小计算指南

### 公式

```
连接数 = ((线程等待时间 + SQL执行时间) / SQL执行时间) * QPS
```

### 实际案例

**场景1: 低频查询**
- QPS: 10
- 平均SQL执行时间: 20ms
- 平均等待时间: 5ms
- 计算: ((5 + 20) / 20) * 10 = 12.5 → 建议 15

**场景2: 高频GPS上报**
- QPS: 100（100辆车每秒上报）
- 平均SQL执行时间: 10ms（简单INSERT）
- 平均等待时间: 2ms
- 计算: ((2 + 10) / 10) * 100 = 120 → 建议 150

**场景3: 混合负载（本项目）**
- GPS上报: 10 QPS（10秒间隔）
- 查询请求: 20 QPS
- 告警处理: 5 QPS
- 总计: 35 QPS
- 平均SQL时间: 15ms
- 计算: ((3 + 15) / 15) * 35 = 42 → 建议 50

**保守配置**: max-active = 20-30（考虑到大部分时间是空闲的）

---

## 监控与调优

### 1. Druid 监控页面

访问: `http://localhost:8080/druid/index.html`

**关键指标:**
- 活跃连接数曲线
- 等待获取连接的线程数
- SQL执行时间分布
- 慢SQL列表

### 2. 应用日志监控

```yaml
logging:
  level:
    com.alibaba.druid: WARN
    com.alibaba.druid.filter.stat.StatFilter: INFO  # 慢SQL日志
```

### 3. 告警规则

**需要告警的情况:**
- 活跃连接数持续 > max-active * 0.8
- 等待获取连接的线程数 > 0
- 慢SQL数量突增
- 连接泄漏次数 > 0

---

## 实施步骤

### 第一步：更新配置文件（立即执行）

1. 修改 `application.yml` 中的 Druid 配置
2. 修改 Druid 监控页面密码
3. 重启应用

### 第二步：观察监控（1周）

1. 每天查看 Druid 监控页面
2. 记录高峰期的连接使用情况
3. 收集慢SQL列表

### 第三步：精细调优（1周后）

根据监控数据调整：
- 如果最大连接数经常用满 → 增加 max-active
- 如果空闲连接很少被使用 → 降低 min-idle
- 如果有大量慢SQL → 优化SQL或索引

---

## 常见问题排查

### 问题1: 获取连接超时

**错误信息:**
```
GetConnectionTimeoutException: wait millis 30000, active 20, maxActive 20
```

**解决方案:**
1. 检查是否有连接泄漏（启用 remove-abandoned）
2. 增加 max-active
3. 优化慢SQL
4. 检查是否有长事务

### 问题2: 连接频繁创建销毁

**现象:**
- Druid 监控显示连接创建次数很高
- 应用性能不稳定

**解决方案:**
1. 增加 min-idle
2. 增加 min-evictable-idle-time-millis
3. 确保 test-while-idle = true

### 问题3: 数据库连接过多

**现象:**
- MySQL show processlist 显示大量连接
- 超过 max_connections 限制

**解决方案:**
1. 降低 max-active
2. 检查是否有多个应用实例共享数据库
3. 检查是否有连接未正确关闭

---

## 最佳实践总结

✅ **应该做的:**
1. 启用连接泄漏检测
2. 设置合理的慢SQL阈值（2秒）
3. 定期查看 Druid 监控页面
4. 使用 preparedStatement 缓存
5. 修改默认的管理员密码

❌ **不应该做的:**
1. 不要设置过大的 max-active（浪费资源）
2. 不要禁用 test-while-idle
3. 不要在borrow/return时验证（影响性能）
4. 不要忽略慢SQL日志
5. 不要在生产环境开放 Druid 监控页面

---

## 相关配置文件

- `src/main/resources/application.yml` - 主配置文件
- Druid 监控: `http://localhost:8080/druid/index.html`
