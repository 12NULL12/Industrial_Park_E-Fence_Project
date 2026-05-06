package com.fence.entity;
//wzj
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeviceAlarmData {
    @JsonProperty("vehicleId")
    private String vehicleId;

    @JsonProperty("alarmType")
    private String alarmType;

    @JsonProperty("message")
    private String message;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("timestamp")
    private String timestamp;
}
