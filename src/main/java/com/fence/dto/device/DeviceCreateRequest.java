package com.fence.dto.device;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class DeviceCreateRequest {

    @NotBlank(message = "设备编号不能为空")
    private String deviceNo;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    private String manufacturer;

    private String model;

    private String firmwareVersion;

    private String simCardNo;

    private String remark;
}
