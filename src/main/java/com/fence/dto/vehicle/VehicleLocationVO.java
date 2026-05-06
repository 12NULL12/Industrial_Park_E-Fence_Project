package com.fence.dto.vehicle;

import lombok.Data;

@Data
public class VehicleLocationVO {
    private Long id;
    private String plate; // 对应前端的 plate
    private Double lng;   // 对应前端的 lng
    private Double lat;   // 对应前端的 lat
    private Double speed;
    private String status;
}
