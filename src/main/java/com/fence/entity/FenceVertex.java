package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FenceVertex {
    private Long id;
    private Long fenceId;                // 围栏ID
    private Double longitude;            // 经度
    private Double latitude;             // 纬度
    private Integer vertexOrder;         // 顶点顺序
    private LocalDateTime createTime;
}
