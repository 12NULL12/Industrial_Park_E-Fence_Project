package com.fence.dto.cargo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargoVerifyVO {
    private Long cargoId;
    private String cargoName;
    private Integer quantity;
    private String unit;
    private String status;
    private String message;
}
