package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ParkingRecord {
    private Long id;
    private String plateNumber;
    private String vehicleType;
    private String action;
    private String status;
    private Long alarmId;
    private LocalDateTime enterTime;
    private LocalDateTime exitTime;
    private Long confirmBy;
    private String confirmName;
    private LocalDateTime confirmTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}