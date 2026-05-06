package com.fence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cargo")
public class Cargo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cargoName;
    private String cargoCode;
    private String cargoType;
    private BigDecimal weight;
    private String unit;
    private Integer quantity;
    private String status;
    private Long sourceWarehouseId;
    private Long destWarehouseId;
    private Long taskId;
    private Long bindVehicleId;
    private Long bindDriverId;
    private LocalDateTime bindTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
