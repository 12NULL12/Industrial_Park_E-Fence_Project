package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Fence {
    private Long id;
    private String fenceType;
    private String fenceName;            // 围栏名称
    private String description;          // 描述
    private Integer alarmLevel;          // 告警级别：1-低 2-中 3-高
    private Boolean enabled;             // 是否启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
