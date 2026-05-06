package com.fence.dto.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AlarmDetailVO {
    private Long id;
    @JsonProperty("driverName")
    private String driverName;
    private String location;
    private String alarmNo;
    private Long vehicleId;
    private String vehiclePlate;
    private Long taskId;
    @JsonProperty("type")
    private String alarmType;
    private String alarmLevel;
    @JsonProperty("detail")
    private String alarmContent;
    private Double longitude;
    private Double latitude;
    private String status;
    private String handleMethod;
    private String handleRemark;
    private Long handlerId;
    private String handlerName;
    @JsonProperty("alarmTime")
    private LocalDateTime alarmTime;
    private LocalDateTime handleTime;
}
