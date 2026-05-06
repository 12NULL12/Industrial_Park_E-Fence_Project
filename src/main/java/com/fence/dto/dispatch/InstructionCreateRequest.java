package com.fence.dto.dispatch;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class InstructionCreateRequest {

    private Long taskId;
    @NotNull(message = "车辆ID不能为空")
    private Long vehicleId;

    private String vehiclePlate;

    private Long deviceId;

    @NotBlank(message = "指令类型不能为空")
    private String instructionType;

    @NotBlank(message = "指令内容不能为空")
    private String instructionContent;
}
