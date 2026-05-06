package com.fence.dto.driver;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DriverDetailVO {

    private Long id;
    private String name;
    private String phone;
    private String idCard;
    private String licenseNumber;
    private String licenseType;
    private LocalDateTime licenseExpiry;
    private Integer age;
    private String gender;
    private String status;
    private Long vehicleId;
    private String vehiclePlate;
    private Double currentLongitude;
    private Double currentLatitude;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime createTime;
}
