package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Alarm {
    private Long id;
    private String alarmNo;              // 告警编号
    private Long vehicleId;              // 车辆ID
    private String vehiclePlate;         // 车牌号
    private Long taskId;                 // 任务ID（可选）
    private String alarmType;            // 告警类型：OUT_FENCE/SPEED_OVERDUE/DEVICE_FAULT等
    private String alarmLevel;           // 告警级别：HIGH/MEDIUM/LOW
    private String alarmContent;         // 告警内容描述
    private Double longitude;            // 告警发生时经度
    private Double latitude;             // 告警发生时纬度
    private String status;               // 状态：PENDING/CONFIRMED/PROCESSING/RESOLVED/FALSE_ALARM
    private String handleMethod;         // 处理方式：ADJUST_ROUTE/TEMP_STOP/EMERGENCY_STOP/IGNORE
    private String handleRemark;         // 处理备注
    private Long handlerId;              // 处理人ID
    private String handlerName;          // 处理人姓名
    private LocalDateTime alarmTime;     // 告警时间
    private LocalDateTime handleTime;    // 处理时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
