-- =====================================================
-- 数据库索引优化脚本
-- 创建时间: 2026-05-25
-- 说明: 为高频查询添加缺失的索引，提升查询性能
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 告警表(alarm)索引优化
-- =====================================================

-- 1.1 为按车辆和告警类型查询最新告警添加复合索引
-- 优化查询: selectLatestByVehicleAndType
ALTER TABLE `alarm` 
ADD INDEX `idx_vehicle_type_time` (`vehicle_id`, `alarm_type`, `alarm_time` DESC);

-- 1.2 为按状态和时间范围查询添加复合索引
-- 优化查询: selectList (status + alarm_time范围查询)
ALTER TABLE `alarm` 
ADD INDEX `idx_status_time` (`status`, `alarm_time` DESC);

-- 1.3 为按告警类型统计添加索引
-- 优化查询: statisticsByType
ALTER TABLE `alarm` 
ADD INDEX `idx_alarm_type` (`alarm_type`);

-- =====================================================
-- 2. 任务表(task)索引优化
-- =====================================================

-- 2.1 为超时任务查询优化（避免使用函数）
-- 原查询: end_time < NOW() 无法使用索引
-- 建议：在应用层计算时间阈值，或使用生成的列
ALTER TABLE `task` 
ADD INDEX `idx_end_time_status` (`end_time`, `status`);

-- 2.2 为多条件查询添加复合索引
-- 优化查询: selectList (status + vehicle_id + driver_id)
ALTER TABLE `task` 
ADD INDEX `idx_status_vehicle_driver` (`status`, `vehicle_id`, `driver_id`);

-- 2.3 为时间范围查询添加索引
ALTER TABLE `task` 
ADD INDEX `idx_start_time` (`start_time`);

ALTER TABLE `task` 
ADD INDEX `idx_create_time_desc` (`create_time` DESC);

-- =====================================================
-- 3. 设备表(device)索引优化
-- =====================================================

-- 3.1 为离线设备查询优化
-- 优化查询: selectOfflineDevices (status + last_heartbeat_time)
ALTER TABLE `device` 
ADD INDEX `idx_status_heartbeat` (`status`, `last_heartbeat_time`);

-- 3.2 为按车辆查询设备添加索引（如果频繁使用）
ALTER TABLE `device` 
ADD INDEX `idx_vehicle_status` (`vehicle_id`, `status`);

-- =====================================================
-- 4. 车辆表(vehicle)索引优化
-- =====================================================

-- 4.1 为按司机查询车辆添加索引（已存在idx_driver_id，确认是否需要复合索引）
-- 如果需要按司机和状态查询
ALTER TABLE `vehicle` 
ADD INDEX `idx_driver_status` (`driver_id`, `status`);

-- 4.2 为位置更新查询优化
ALTER TABLE `vehicle` 
ADD INDEX `idx_last_update` (`last_update_time`);

-- =====================================================
-- 5. 围栏相关表索引优化
-- =====================================================

-- 5.1 fence_vertex表已有idx_fence_id，无需额外优化

-- 5.2 fence_vehicle表已有uk_fence_vehicle和idx_vehicle_id，无需额外优化

-- =====================================================
-- 6. 操作日志表(operation_log)索引优化
-- =====================================================

-- 6.1 为按模块和操作类型查询添加索引
ALTER TABLE `operation_log` 
ADD INDEX `idx_module_operation` (`module`, `operation`);

-- 6.2 为按用户和时间范围查询优化（已有idx_user_id和idx_create_time）
-- 如果需要联合查询，可以添加复合索引
ALTER TABLE `operation_log` 
ADD INDEX `idx_user_time` (`user_id`, `create_time` DESC);

-- =====================================================
-- 7. 司机表(driver)索引优化
-- =====================================================

-- 7.1 为按状态查询司机添加索引
ALTER TABLE `driver` 
ADD INDEX `idx_status_available` (`status`);

-- =====================================================
-- 验证索引创建结果
-- =====================================================

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'vehicle_fence'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 完成提示
SELECT '✅ 索引优化脚本执行完成！' AS message;
