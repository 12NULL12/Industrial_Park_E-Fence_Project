package com.fence.dto.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LogDetailVO {

    private Long id;
    private String logType;
    private String module;
    @JsonProperty("action")
    private String operation;
    private Long operatorId;
    @JsonProperty("operator")
    private String operatorName;
    private String requestMethod;
    private String requestUrl;
    @JsonProperty("content")
    private String requestParams;
    @JsonProperty("ip")
    private String ipAddress;
    private String userAgent;
    private Integer responseStatus;
    private String errorMsg;
    private Long executeTime;
    @JsonProperty("time")
    private LocalDateTime createTime;
    
    // 登录日志特有字段
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("status")
    private String loginStatus;
    
    @JsonProperty("location")
    private String location;
    
    // 调度日志特有字段
    @JsonProperty("vehiclePlate")
    private String vehiclePlate;
    
    @JsonProperty("instructionType")
    private String instructionType;
}
