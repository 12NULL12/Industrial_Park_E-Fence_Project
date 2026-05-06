package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Device {
    private Long id;
    private String deviceNo;                 // 设备编号（IMEI/SN）
    private String deviceType;               // 设备类型：GPS_TRACKER/OBD/CAMERA
    private String manufacturer;             // 制造商
    private String model;                    // 型号
    private String firmwareVersion;          // 固件版本
    private Long vehicleId;                  // 绑定车辆ID
    private String vehiclePlate;             // 绑定车辆车牌号
    private String status;                   // 状态：ONLINE/OFFLINE/FAULT/MAINTENANCE
    private Double lastLongitude;            // 最后上报经度
    private Double lastLatitude;             // 最后上报纬度
    private LocalDateTime lastOnlineTime;    // 最后在线时间
    private LocalDateTime lastHeartbeatTime; // 最后心跳时间
    private Integer batteryLevel;            // 电池电量（百分比）
    private String simCardNo;                // SIM卡号
    private String remark;                   // 备注
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
