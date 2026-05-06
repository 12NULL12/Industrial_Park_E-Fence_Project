package com.fence.dto.fence;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FenceDetailVO {

    private Long id;
    private String fenceName;
    private String fenceType;
    private String description;
    private Integer alarmLevel;
    private Boolean enabled;
    private List<VertexVO> vertices;
    
    /**
     * 专门给前端提供的 coordinates 字段
     * 格式: "[[lng,lat],[lng,lat],...]"
     */
    @JsonProperty("coordinates")
    public String getCoordinates() {
        if (vertices == null || vertices.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vertices.size(); i++) {
            VertexVO v = vertices.get(i);
            sb.append("[").append(v.getLongitude()).append(",").append(v.getLatitude()).append("]");
            if (i < vertices.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private List<Long> boundVehicleIds;
    private LocalDateTime createTime;

    @Data
    public static class VertexVO {
        private Double longitude;
        private Double latitude;
        private Integer vertexOrder;
    }
}
