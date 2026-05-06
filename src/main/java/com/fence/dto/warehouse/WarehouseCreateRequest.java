// WarehouseCreateRequest.java（仓库创建请求DTO）
package com.fence.dto.warehouse;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WarehouseCreateRequest {
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Long adminId;
}
