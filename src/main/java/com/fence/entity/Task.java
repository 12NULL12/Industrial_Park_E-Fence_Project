package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Task {
    private Long id;
    private String taskNo;                 // 任务编号
    private String taskName;               // 任务名称
    private Long vehicleId;                // 车辆ID
    private String vehiclePlate;           // 车牌号
    private Long driverId;                 // 司机ID
    private String driverName;             // 司机姓名
    private Long startWarehouseId;         // 起始仓库ID
    private String startWarehouseName;     // 起始仓库名称
    private Long endWarehouseId;           // 目的仓库ID
    private String endWarehouseName;       // 目的仓库名称
    private String status;                 // 状态：PENDING/ASSIGNED/IN_PROGRESS/COMPLETED/CANCELLED/TIMEOUT
    private LocalDateTime startTime;       // 计划开始时间
    private LocalDateTime endTime;         // 计划结束时间
    private LocalDateTime actualStartTime; // 实际开始时间
    private LocalDateTime actualEndTime;   // 实际结束时间
    private String remark;                 // 备注
    private Integer priority;              // 优先级：1-低 2-中 3-高
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
