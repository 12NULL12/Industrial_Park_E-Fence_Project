package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FenceVehicle {
    private Long id;
    private Long fenceId;                // 围栏ID
    private Long vehicleId;              // 车辆ID
    private LocalDateTime bindTime;      // 绑定时间
    private LocalDateTime createTime;
}
