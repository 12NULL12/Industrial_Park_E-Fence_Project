package com.fence.dto.vehicle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VehicleDetailVO {

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
    @JsonProperty("lng")
    private Double currentLongitude;
    @JsonProperty("lat")
    private Double currentLatitude;
    private Integer speed;
    @JsonProperty("lastUpdate")
    private LocalDateTime lastUpdateTime;
    private LocalDateTime createTime;
}
