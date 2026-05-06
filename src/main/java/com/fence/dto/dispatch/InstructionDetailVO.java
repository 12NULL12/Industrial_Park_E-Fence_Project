package com.fence.dto.dispatch;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstructionDetailVO {

    private Long id;
    private String instructionNo;
    private Long taskId;
    private String taskNo;
    private Long vehicleId;
    private String vehiclePlate;
    private Long deviceId;
    private String instructionType;
    private String instructionContent;
    private String status;
    private LocalDateTime sendTime;
    private LocalDateTime receiveTime;
    private LocalDateTime executeTime;
    private LocalDateTime completeTime;
    private String feedback;
    private String failReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
