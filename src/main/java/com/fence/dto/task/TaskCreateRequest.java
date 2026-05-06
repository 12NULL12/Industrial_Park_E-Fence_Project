package com.fence.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "起始仓库ID不能为空")
    private Long startWarehouseId;

    private String startWarehouseName;

    @NotNull(message = "目的仓库ID不能为空")
    private Long endWarehouseId;

    private String endWarehouseName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String remark;

    private Integer priority;

    private List<CargoItem> cargos;

    @Data
    public static class CargoItem {
        private Long cargoId;
        private String cargoName;
        private java.math.BigDecimal weight;
        private java.math.BigDecimal volume;
        private Integer quantity;
        private String unit;
    }
}