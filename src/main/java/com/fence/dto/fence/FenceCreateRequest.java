package com.fence.dto.fence;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class FenceCreateRequest {

    @NotBlank(message = "围栏名称不能为空")
    private String fenceName;

    @NotBlank(message = "围栏类型不能为空")
    private String fenceType;

    private String description;

    private Integer alarmLevel;

    private Boolean enabled;

    /**
     * 围栏顶点坐标（仅在创建或修改围栏形状时需要）
     * 如果为 null，表示不修改围栏形状
     */
    private List<Coordinate> vertices;

    /**
     * 绑定的车辆ID列表
     * 如果为 null，表示不修改绑定关系
     */
    private List<Long> boundVehicleIds;

    @Data
    public static class Coordinate {
        private Double longitude;
        private Double latitude;
    }
}