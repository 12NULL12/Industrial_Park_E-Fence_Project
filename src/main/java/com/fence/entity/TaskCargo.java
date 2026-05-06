package com.fence.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskCargo {
    private Long id;
    private Long taskId;                 // 任务ID
    private Long cargoId;                // 货物ID
    private String cargoName;            // 货物名称
    private BigDecimal weight;           // 重量（吨）
    private BigDecimal volume;           // 体积（立方米）
    private Integer quantity;            // 数量
    private String unit;                 // 单位
    private LocalDateTime createTime;
}
