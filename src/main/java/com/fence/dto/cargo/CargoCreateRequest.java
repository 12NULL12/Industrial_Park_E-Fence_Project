package com.fence.dto.cargo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CargoCreateRequest {
    private String name;
    private String code;
    private String type;
    private BigDecimal weight;
    private String unit;
    private Integer quantity;
    private Long sourceWarehouseId;
    private Long destWarehouseId;
}
