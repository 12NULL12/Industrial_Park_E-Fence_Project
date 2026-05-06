package com.fence.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private String logType;                // 日志类型：LOGIN/OPERATION/DISPATCH/SYSTEM
    private String module;                 // 模块名称：VEHICLE/DRIVER/TASK/FENCE等
    private String operation;              // 操作描述：创建车辆/分配任务等
    private Long operatorId;               // 操作人ID
    private String operatorName;           // 操作人姓名
    private String requestMethod;          // 请求方法：GET/POST/PUT/DELETE
    private String requestUrl;             // 请求URL
    private String requestParams;          // 请求参数（JSON）
    private String ipAddress;              // IP地址
    private String userAgent;              // 用户代理
    private Integer responseStatus;        // 响应状态码
    private String errorMsg;               // 错误信息
    private Long executeTime;              // 执行耗时（毫秒）
    private LocalDateTime createTime;
}
