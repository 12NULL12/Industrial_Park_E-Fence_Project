package com.fence.dto.cargo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CargoDetailVO {
    private Long id;
    private String name;
    private String code;
    private String type;
    private BigDecimal weight;
    private String unit;
    private Integer quantity;
    private String status;
    private String sourceWarehouse;
    private String destWarehouse;
}
