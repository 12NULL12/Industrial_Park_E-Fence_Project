package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Driver{
    private Long id;
    private String userId;
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
    private LocalDateTime updateTime;
}
