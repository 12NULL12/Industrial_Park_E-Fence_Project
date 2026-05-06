package com.fence.entity;
//wzj
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HeartbeatData {
    @JsonProperty("vehicleId")
    private String vehicleId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timestamp")
    private String timestamp;
}