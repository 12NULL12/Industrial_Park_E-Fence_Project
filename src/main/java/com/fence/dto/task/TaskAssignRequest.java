package com.fence.dto.task;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class TaskAssignRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "车辆ID不能为空")
    private Long vehicleId;

    private String vehiclePlate;

    private Long driverId;

    private String driverName;
}
