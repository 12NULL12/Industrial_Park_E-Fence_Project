package com.fence.dto.device;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class DeviceUpdateRequest {

    @NotNull(message = "设备ID不能为空")
    private Long id;

    private String deviceType;

    private String manufacturer;

    private String model;

    private String firmwareVersion;

    private String simCardNo;

    private String status;

    private String remark;
}
