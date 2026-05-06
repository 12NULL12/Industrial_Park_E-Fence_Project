-- =====================================================
-- 电子围栏车辆追踪预警系统 - 数据库初始化脚本
-- 数据库: vehicle_fence
-- 创建时间: 2026-04-20
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 用户表
-- =====================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` VARCHAR(50) NOT NULL COMMENT '用户ID',
                        `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                        `password` VARCHAR(100) NOT NULL COMMENT '密码',
                        `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 司机表
-- =====================================================
DROP TABLE IF EXISTS `driver`;
CREATE TABLE `driver` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '司机ID',
                          `user_id` VARCHAR(50) NOT NULL COMMENT '关联用户ID',
                          `driver_name` VARCHAR(50) NOT NULL COMMENT '司机姓名',
                          `license_no` VARCHAR(50) DEFAULT NULL COMMENT '驾驶证号',
                          `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
                          `status` VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态',
                          `vehicle_id` BIGINT DEFAULT NULL COMMENT '当前绑定车辆ID',
                          `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '当前绑定车辆车牌',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_user_id` (`user_id`),
                          KEY `idx_vehicle_id` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='司机表';

-- =====================================================
-- 3. 车辆表
-- =====================================================
DROP TABLE IF EXISTS `vehicle`;
CREATE TABLE `vehicle` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
                           `plate_number` VARCHAR(20) NOT NULL COMMENT '车牌号',
                           `vehicle_type` VARCHAR(50) DEFAULT NULL COMMENT '车辆类型',
                           `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
                           `model` VARCHAR(50) DEFAULT NULL COMMENT '型号',
                           `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色',
                           `load_capacity` INT DEFAULT NULL COMMENT '载重吨位',
                           `status` VARCHAR(20) DEFAULT 'IDLE' COMMENT '状态',
                           `device_id` BIGINT DEFAULT NULL COMMENT '关联设备ID',
                           `driver_id` BIGINT DEFAULT NULL COMMENT '当前司机ID',
                           `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '当前司机姓名',
                           `current_longitude` DOUBLE DEFAULT NULL COMMENT '当前经度',
                           `current_latitude` DOUBLE DEFAULT NULL COMMENT '当前纬度',
                           `speed` DOUBLE DEFAULT 0 COMMENT '当前速度',
                           `last_update_time` DATETIME DEFAULT NULL COMMENT '最后更新时间',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_plate_number` (`plate_number`),
                           KEY `idx_driver_id` (`driver_id`),
                           KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆表';

-- =====================================================
-- 4. 设备表
-- =====================================================
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
                          `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
                          `device_no` VARCHAR(50) NOT NULL COMMENT '设备编号',
                          `device_type` VARCHAR(50) DEFAULT 'GPS_TRACKER' COMMENT '设备类型',
                          `manufacturer` VARCHAR(100) DEFAULT NULL COMMENT '制造商',
                          `model` VARCHAR(50) DEFAULT NULL COMMENT '型号',
                          `firmware_version` VARCHAR(50) DEFAULT NULL COMMENT '固件版本',
                          `vehicle_id` BIGINT DEFAULT NULL COMMENT '绑定车辆ID',
                          `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '绑定车辆车牌',
                          `status` VARCHAR(20) DEFAULT 'OFFLINE' COMMENT '状态',
                          `last_longitude` DOUBLE DEFAULT NULL COMMENT '最后经度',
                          `last_latitude` DOUBLE DEFAULT NULL COMMENT '最后纬度',
                          `last_online_time` DATETIME DEFAULT NULL COMMENT '最后在线时间',
                          `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
                          `battery_level` INT DEFAULT NULL COMMENT '电池电量',
                          `sim_card_no` VARCHAR(50) DEFAULT NULL COMMENT 'SIM卡号',
                          `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                          `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                          `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_device_no` (`device_no`),
                          KEY `idx_vehicle_id` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- =====================================================
-- 5. 围栏表
-- =====================================================
DROP TABLE IF EXISTS `fence`;
CREATE TABLE `fence` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '围栏ID',
                         `fence_name` VARCHAR(100) NOT NULL COMMENT '围栏名称',
                         `fence_type` VARCHAR(20) NOT NULL COMMENT '围栏类型：FORBIDDEN/ALLOWED/ROUTE',
                         `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
                         `alarm_level` INT DEFAULT 2 COMMENT '告警级别：1-低 2-中 3-高',
                         `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
                         `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                         `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`),
                         KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='围栏表';

-- =====================================================
-- 6. 围栏顶点表
-- =====================================================
DROP TABLE IF EXISTS `fence_vertex`;
CREATE TABLE `fence_vertex` (
                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '顶点ID',
                                `fence_id` BIGINT NOT NULL COMMENT '围栏ID',
                                `longitude` DOUBLE NOT NULL COMMENT '经度',
                                `latitude` DOUBLE NOT NULL COMMENT '纬度',
                                `vertex_order` INT NOT NULL COMMENT '顶点顺序',
                                `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `idx_fence_id` (`fence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='围栏顶点表';

-- =====================================================
-- 7. 围栏-车辆关联表
-- =====================================================
DROP TABLE IF EXISTS `fence_vehicle`;
CREATE TABLE `fence_vehicle` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
                                 `fence_id` BIGINT NOT NULL COMMENT '围栏ID',
                                 `vehicle_id` BIGINT NOT NULL COMMENT '车辆ID',
                                 `bind_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_fence_vehicle` (`fence_id`, `vehicle_id`),
                                 KEY `idx_vehicle_id` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='围栏车辆关联表';

-- =====================================================
-- 8. 告警表
-- =====================================================
DROP TABLE IF EXISTS `alarm`;
CREATE TABLE `alarm` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警ID',
                         `alarm_no` VARCHAR(50) NOT NULL COMMENT '告警编号',
                         `vehicle_id` BIGINT NOT NULL COMMENT '车辆ID',
                         `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
                         `task_id` BIGINT DEFAULT NULL COMMENT '任务ID',
                         `alarm_type` VARCHAR(50) NOT NULL COMMENT '告警类型',
                         `alarm_level` VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '告警级别',
                         `alarm_content` VARCHAR(500) DEFAULT NULL COMMENT '告警内容',
                         `longitude` DOUBLE DEFAULT NULL COMMENT '告警经度',
                         `latitude` DOUBLE DEFAULT NULL COMMENT '告警纬度',
                         `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
                         `handle_method` VARCHAR(50) DEFAULT NULL COMMENT '处理方式',
                         `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
                         `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
                         `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名',
                         `alarm_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
                         `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
                         `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                         `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_alarm_no` (`alarm_no`),
                         KEY `idx_vehicle_id` (`vehicle_id`),
                         KEY `idx_status` (`status`),
                         KEY `idx_alarm_time` (`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警表';

-- =====================================================
-- 9. 任务表
-- =====================================================
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                        `task_no` VARCHAR(50) NOT NULL COMMENT '任务编号',
                        `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
                        `vehicle_id` BIGINT DEFAULT NULL COMMENT '车辆ID',
                        `vehicle_plate` VARCHAR(20) DEFAULT NULL COMMENT '车牌号',
                        `driver_id` BIGINT DEFAULT NULL COMMENT '司机ID',
                        `driver_name` VARCHAR(50) DEFAULT NULL COMMENT '司机姓名',
                        `start_warehouse_id` BIGINT DEFAULT NULL COMMENT '起始仓库ID',
                        `start_warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '起始仓库名称',
                        `end_warehouse_id` BIGINT DEFAULT NULL COMMENT '目的仓库ID',
                        `end_warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '目的仓库名称',
                        `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
                        `start_time` DATETIME DEFAULT NULL COMMENT '计划开始时间',
                        `end_time` DATETIME DEFAULT NULL COMMENT '计划结束时间',
                        `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
                        `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
                        `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                        `priority` INT DEFAULT 2 COMMENT '优先级',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_task_no` (`task_no`),
                        KEY `idx_vehicle_id` (`vehicle_id`),
                        KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- =====================================================
-- 10. 操作日志表
-- =====================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                                 `user_id` VARCHAR(50) DEFAULT NULL COMMENT '操作用户ID',
                                 `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
                                 `operation` VARCHAR(100) NOT NULL COMMENT '操作类型',
                                 `module` VARCHAR(50) DEFAULT NULL COMMENT '模块',
                                 `content` VARCHAR(1000) DEFAULT NULL COMMENT '操作内容',
                                 `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (`id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 初始化测试数据
-- =====================================================

-- 1. 插入调度员用户
INSERT INTO `user` (`id`, `username`, `password`, `phone`) VALUES
    ('1', 'dispatcher', '123456', '13800138000');

-- 2. 插入测试司机
INSERT INTO `driver` (`user_id`, `driver_name`, `license_no`, `phone`, `status`) VALUES
    ('driver001', '张三', '110101199001011234', '13900139001', 'AVAILABLE');

-- 3. 插入测试车辆
INSERT INTO `vehicle` (`plate_number`, `vehicle_type`, `brand`, `model`, `color`, `status`, `driver_id`, `driver_name`) VALUES
    ('京A88888', 'TRUCK', '解放', 'J6P', '红色', 'RUNNING', 1, '张三');

-- 4. 插入测试设备
INSERT INTO `device` (`device_no`, `device_type`, `vehicle_id`, `vehicle_plate`, `status`) VALUES
    ('DEV001', 'GPS_TRACKER', 1, '京A88888', 'ONLINE');

-- 5. 更新车辆绑定设备
UPDATE `vehicle` SET `device_id` = 1 WHERE `id` = 1;

-- 6. 插入测试围栏（禁区 - 矩形）
INSERT INTO `fence` (`fence_name`, `fence_type`, `description`, `alarm_level`, `enabled`) VALUES
    ('测试禁区', 'FORBIDDEN', '北京市朝阳区测试禁区', 2, 1);

-- 7. 插入围栏顶点（矩形四个角点）
INSERT INTO `fence_vertex` (`fence_id`, `longitude`, `latitude`, `vertex_order`) VALUES
                                                                                     (1, 116.400, 39.900, 1),
                                                                                     (1, 116.410, 39.900, 2),
                                                                                     (1, 116.410, 39.910, 3),
                                                                                     (1, 116.400, 39.910, 4);

-- 8. 绑定车辆到围栏
INSERT INTO `fence_vehicle` (`fence_id`, `vehicle_id`) VALUES
    (1, 1);

-- 完成提示
SELECT '✅ 数据库初始化完成！' AS message;
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'vehicle_fence';
