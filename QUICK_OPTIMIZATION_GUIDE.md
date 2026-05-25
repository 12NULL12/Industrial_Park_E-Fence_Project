# 数据库优化快速执行指南

## 🎯 5分钟快速开始

### 第一步：备份数据库（必须！）

```powershell
# PowerShell 命令
mysqldump -u root -p vehicle_fence > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
```

或者在 MySQL 客户端执行：
```bash
mysqldump -u root -p vehicle_fence > backup_before_optimization.sql
```

---

### 第二步：执行索引优化脚本

```powershell
# 进入项目目录
cd F:\mavendemo\elderly-care-user-servic

# 执行索引脚本
mysql -u root -p vehicle_fence < src\main\resources\optimize-indexes.sql
```

**预期输出:**
```
✅ 索引优化脚本执行完成！
```

---

### 第三步：重启应用

```powershell
# 如果应用正在运行，先停止
# 然后重新启动
mvn spring-boot:run
```

---

### 第四步：验证优化效果

1. **访问 Druid 监控页面**
   - URL: `http://localhost:8080/druid/index.html`
   - 用户名: `admin`
   - 密码: `admin@123`

2. **检查关键指标**
   - ✅ 活跃连接数正常（应该 < 20）
   - ✅ 没有等待获取连接的线程
   - ✅ SQL执行时间在合理范围

3. **测试主要接口**
   ```bash
   # 测试告警列表
   curl http://localhost:8080/api/alarms?pageNum=1&pageSize=10
   
   # 测试任务列表
   curl http://localhost:8080/api/tasks?pageNum=1&pageSize=10
   
   # 测试设备列表
   curl http://localhost:8080/api/devices?pageNum=1&pageSize=10
   ```

4. **查看慢SQL日志**
   - Druid 监控 → SQL监控 → 慢SQL
   - 应该看到阈值已改为 2000ms

---

## 🔍 验证索引是否创建成功

登录 MySQL 执行以下命令：

```sql
-- 查看告警表索引
SHOW INDEX FROM alarm;

-- 应该看到新增的索引：
-- idx_vehicle_type_time
-- idx_status_time
-- idx_alarm_type

-- 查看任务表索引
SHOW INDEX FROM task;

-- 应该看到新增的索引：
-- idx_end_time_status
-- idx_status_vehicle_driver
-- idx_start_time
-- idx_create_time_desc

-- 查看设备表索引
SHOW INDEX FROM device;

-- 应该看到新增的索引：
-- idx_status_heartbeat
-- idx_vehicle_status
```

---

## 📊 性能对比测试

### 测试1: 告警查询性能

**优化前（无索引）:**
```sql
EXPLAIN SELECT * FROM alarm 
WHERE vehicle_id = 1 AND alarm_type = 'OUT_FENCE' 
ORDER BY alarm_time DESC LIMIT 1;
-- type: ALL (全表扫描)
-- rows: 100000+
```

**优化后（有索引）:**
```sql
EXPLAIN SELECT * FROM alarm 
WHERE vehicle_id = 1 AND alarm_type = 'OUT_FENCE' 
ORDER BY alarm_time DESC LIMIT 1;
-- type: ref (索引查找)
-- rows: 1-10
```

### 测试2: 分页查询性能

**优化前:**
```sql
SELECT * FROM alarm ORDER BY alarm_time DESC LIMIT 10 OFFSET 10000;
-- 执行时间: 500ms+
```

**优化后（使用延迟关联或游标分页）:**
```sql
-- 游标分页
SELECT * FROM alarm 
WHERE alarm_time < '2026-05-25 10:30:00'
ORDER BY alarm_time DESC LIMIT 10;
-- 执行时间: 3ms
```

---

## ⚠️ 常见问题排查

### 问题1: 索引创建失败

**错误信息:**
```
ERROR 1061 (42000): Duplicate key name 'idx_vehicle_type_time'
```

**原因**: 索引已存在

**解决**: 可以忽略，说明索引已经创建过

---

### 问题2: 应用启动失败

**错误信息:**
```
Failed to obtain JDBC Connection
```

**原因**: 数据库连接配置错误

**解决**:
1. 检查 `application.yml` 中的数据库配置
2. 确认 MySQL 服务正在运行
3. 验证用户名密码正确

---

### 问题3: Druid 监控页面无法访问

**可能原因:**
1. 应用未启动
2. 端口被占用
3. 路径错误

**解决**:
```powershell
# 检查应用是否运行
netstat -ano | findstr :8080

# 检查日志
type logs\elderly-care-service.log
```

---

## 📈 监控建议

### 每天检查（第一周）

1. **Druid 监控页面**
   - 活跃连接数曲线
   - 慢SQL数量
   - SQL执行时间分布

2. **应用日志**
   ```powershell
   # 查看最新的慢SQL日志
   Get-Content logs\elderly-care-service.log | Select-String "slow sql"
   ```

3. **MySQL 进程列表**
   ```sql
   SHOW PROCESSLIST;
   -- 检查是否有长时间运行的查询
   ```

### 每周分析

1. **导出慢SQL统计**
   - Druid 监控 → SQL监控 → 导出

2. **分析高频查询**
   - 找出执行次数最多的SQL
   - 检查是否需要进一步优化

3. **调整连接池配置**
   - 如果最大连接数经常用满 → 增加 max-active
   - 如果空闲连接很少使用 → 降低 min-idle

---

## 🔄 回滚方案（如果需要）

### 删除新增的索引

```sql
-- 告警表
ALTER TABLE alarm DROP INDEX idx_vehicle_type_time;
ALTER TABLE alarm DROP INDEX idx_status_time;
ALTER TABLE alarm DROP INDEX idx_alarm_type;

-- 任务表
ALTER TABLE task DROP INDEX idx_end_time_status;
ALTER TABLE task DROP INDEX idx_status_vehicle_driver;
ALTER TABLE task DROP INDEX idx_start_time;
ALTER TABLE task DROP INDEX idx_create_time_desc;

-- 设备表
ALTER TABLE device DROP INDEX idx_status_heartbeat;
ALTER TABLE device DROP INDEX idx_vehicle_status;

-- 车辆表
ALTER TABLE vehicle DROP INDEX idx_driver_status;
ALTER TABLE vehicle DROP INDEX idx_last_update;

-- 操作日志表
ALTER TABLE operation_log DROP INDEX idx_module_operation;
ALTER TABLE operation_log DROP INDEX idx_user_time;

-- 司机表
ALTER TABLE driver DROP INDEX idx_status_available;
```

### 恢复配置文件

```powershell
# 如果使用 Git，可以恢复 application.yml
git checkout src/main/resources/application.yml

# 重启应用
mvn spring-boot:run
```

---

## 📞 需要帮助？

### 诊断命令

```sql
-- 查看所有表的索引使用情况
SELECT 
    table_name,
    index_name,
    seq_in_index,
    column_name
FROM information_schema.statistics
WHERE table_schema = 'vehicle_fence'
ORDER BY table_name, index_name, seq_in_index;

-- 查看当前正在执行的查询
SHOW FULL PROCESSLIST;

-- 查看表的大小和行数
SELECT 
    table_name,
    table_rows,
    ROUND(data_length / 1024 / 1024, 2) AS data_size_mb,
    ROUND(index_length / 1024 / 1024, 2) AS index_size_mb
FROM information_schema.tables
WHERE table_schema = 'vehicle_fence'
ORDER BY table_rows DESC;
```

### 性能分析

```sql
-- 分析特定查询的执行计划
EXPLAIN ANALYZE 
SELECT * FROM alarm 
WHERE vehicle_id = 1 
  AND alarm_type = 'OUT_FENCE' 
ORDER BY alarm_time DESC 
LIMIT 10;
```

---

## ✅ 优化完成检查清单

- [ ] 数据库已备份
- [ ] 索引脚本执行成功
- [ ] 应用重启成功
- [ ] Druid 监控页面可访问
- [ ] 主要接口响应正常
- [ ] 慢SQL阈值已调整为2秒
- [ ] 连接泄漏检测已启用
- [ ] 索引创建验证通过
- [ ] 性能测试完成
- [ ] 监控配置完成

---

**祝您优化顺利！🚀**

如有问题，请查看详细文档：
- `DATABASE_OPTIMIZATION_SUMMARY.md` - 完整优化报告
- `PAGINATION_OPTIMIZATION_GUIDE.md` - 分页优化详解
- `CONNECTION_POOL_OPTIMIZATION.md` - 连接池优化详解
