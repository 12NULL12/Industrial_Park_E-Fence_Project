package com.fence.entity;
//wzj
import lombok.Data;

/**
 * 指令载体
 * 用于远程控制指令的传输
 */
@Data
public class CommandPayload {

    private String commandId;
    private String type;
    private String vehicleId;
    private String reason;
    private String operatorId;
    private long timestamp;
}
