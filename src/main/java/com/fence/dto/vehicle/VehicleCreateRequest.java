package com.fence.dto.vehicle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class VehicleCreateRequest {

    @NotBlank(message = "车牌号不能为空")
    @JsonProperty("plate")
    private String plateNumber;

    @NotBlank(message = "车辆类型不能为空")
    @JsonProperty("type")
    private String vehicleType;

    private String brand;

    private String model;

    private String color;

    private Integer loadCapacity;

    private Long deviceId;
}
