package com.fence.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GpsData {

    @JsonProperty("vehicleId")
    private Long vehicleId;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("speed")
    private Double speed;

    @JsonProperty("direction")
    private Integer direction;

    @JsonProperty("timestamp")
    private String timestamp;
}
