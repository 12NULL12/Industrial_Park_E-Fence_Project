package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Instruction {
    private Long id;
    private String commandId;
    private String instructionNo;          // 指令编号
    private Long taskId;                   // 关联任务ID
    private String taskNo;                 // 任务编号
    private Long vehicleId;                // 车辆ID
    private String vehiclePlate;           // 车牌号
    private Long deviceId;                 // 设备ID
    private String instructionType;        // 指令类型：ADJUST_ROUTE/TEMP_STOP/EMERGENCY_STOP/RETURN
    private String instructionContent;     // 指令内容详情
    private String status;                 // 状态：PENDING/SENT/RECEIVED/EXECUTING/COMPLETED/FAILED
    private LocalDateTime sendTime;        // 发送时间
    private LocalDateTime receiveTime;     // 接收时间
    private LocalDateTime executeTime;     // 执行时间
    private LocalDateTime completeTime;    // 完成时间
    private String feedback;               // 司机反馈
    private String failReason;             // 失败原因
    private Long operatorId;               // 操作人ID
    private String operatorName;           // 操作人姓名
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
