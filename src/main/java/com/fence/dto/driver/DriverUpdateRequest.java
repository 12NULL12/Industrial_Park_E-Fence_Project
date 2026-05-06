package com.fence.dto.driver;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class DriverUpdateRequest {

    @NotNull(message = "司机ID不能为空")
    private Long id;

    private String name;

    private String phone;

    private String idCard;

    private String licenseNumber;

    private String licenseType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime licenseExpiry;

    private Integer age;

    private String gender;

    private String status;
}
