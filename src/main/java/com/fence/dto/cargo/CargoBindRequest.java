package com.fence.dto.cargo;

import lombok.Data;

@Data
public class CargoBindRequest {
    private Long taskId;
    private String cargoCode;
}
