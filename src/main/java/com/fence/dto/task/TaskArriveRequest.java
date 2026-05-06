package com.fence.dto.task;

import lombok.Data;

@Data
public class TaskArriveRequest {
    private Long taskId;
    private String actualTime;
}
