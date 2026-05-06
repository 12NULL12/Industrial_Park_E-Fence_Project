package com.fence.dto.device;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeviceDetailVO {

    private Long id;
    private String deviceNo;
    private String deviceType;
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    private Long vehicleId;
    private String vehiclePlate;
    private String status;
    public String getStatus() {
        return status != null ? status.toLowerCase() : null;
    }
    private Double lastLongitude;
    private Double lastLatitude;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime lastHeartbeatTime;
    private Integer batteryLevel;
    private String simCardNo;
    private String remark;
    private LocalDateTime createTime;
}
