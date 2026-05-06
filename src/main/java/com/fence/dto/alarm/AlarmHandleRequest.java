package com.fence.dto.alarm;

import lombok.Data;
import  jakarta.validation.constraints.NotBlank;
import  jakarta.validation.constraints.NotNull;

@Data
public class AlarmHandleRequest {
    @NotNull(message = "告警ID不能为空")
    private Long alarmId;

    @NotBlank(message = "处理方式不能为空")
    private String handleMethod;  // ADJUST_ROUTE/TEMP_STOP/EMERGENCY_STOP/IGNORE

    private String handleRemark;  // 处理备注
}
