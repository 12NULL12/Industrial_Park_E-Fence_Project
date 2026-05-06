package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Vehicle {
    private Long id;
    private String plateNumber;        // 车牌号
    private String vehicleType;        // 车辆类型
    private String brand;              // 品牌
    private String model;              // 型号
    private String color;              // 颜色
    private Integer loadCapacity;      // 载重吨位
    private String status;             // 状态：IDLE/RUNNING/MAINTENANCE/OFFLINE
    private Long deviceId;             // 关联设备ID
    private Long driverId;             // 当前司机ID
    private String driverName;         // 当前司机姓名
    private Double currentLongitude;   // 当前经度
    private Double currentLatitude;    // 当前纬度
    private LocalDateTime lastUpdateTime; // 最后更新时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private double speed;
}
