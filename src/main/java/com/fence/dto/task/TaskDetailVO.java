package com.fence.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailVO {

    private Long id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    private String taskNo;
    private String taskName;
    private Long vehicleId;
    private String vehiclePlate;
    private Long driverId;
    private String driverName;
    private Long startWarehouseId;
    private String startWarehouseName;
    private Long endWarehouseId;
    private String endWarehouseName;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonProperty("completeTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime actualEndTime;

    private String remark;
    private String priority;
    private List<CargoVO> cargos;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @com.fasterxml.jackson.annotation.JsonProperty("status")
    public String getStatus() {
        return status != null ? status.toLowerCase() : null;
    }

    @JsonProperty("priority")
    public String getPriority() {
        return priority != null ? priority.toLowerCase() : null;
    }

    @Data
    public static class CargoVO {
        private Long cargoId;
        private String cargoName;
        private java.math.BigDecimal weight;
        private java.math.BigDecimal volume;
        private Integer quantity;
        private String unit;
    }
}
