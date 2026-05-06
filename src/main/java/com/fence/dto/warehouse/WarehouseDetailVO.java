package com.fence.dto.warehouse;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WarehouseDetailVO {
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String adminName;
}
