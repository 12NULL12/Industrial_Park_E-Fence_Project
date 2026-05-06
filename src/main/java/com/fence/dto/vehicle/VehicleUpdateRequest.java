package com.fence.dto.vehicle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class VehicleUpdateRequest {
    private Long id;

    @JsonProperty("plate")
    private String plateNumber;

    @JsonProperty("type")
    private String vehicleType;

    private String brand;

    private String model;

    private String color;

    private Integer loadCapacity;

    private String status;

    private Long deviceId;

    private Long driverId;

    private String driverName;
}
