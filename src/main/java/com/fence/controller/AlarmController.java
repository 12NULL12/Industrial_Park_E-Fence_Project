package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.alarm.AlarmDetailVO;
import com.fence.dto.alarm.AlarmHandleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import  jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alarm")
public class AlarmController {

    @Autowired
    private com.fence.service.AlarmService alarmService;

    /**
     * 查询告警列表
     */
    @GetMapping("/list")
    public Result<Map<String,Object>> queryAlarms(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alarmType,
            @RequestParam(required = false) String alarmLevel,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;
        List<AlarmDetailVO> list = alarmService.queryAlarms(maxLimit,status, alarmType, alarmLevel, vehicleId, startTime, endTime);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 查询告警详情
     */
    @GetMapping("/{id}/handle")
    public Result<AlarmDetailVO> getAlarmDetail(@PathVariable Long id) {
        AlarmDetailVO detail = alarmService.getAlarmDetail(id);
        return Result.success(detail);
    }

    /**
     * 处理告警
     */
    @PostMapping("{id}/handle")
    public Result<Void> handleAlarm(@Valid @RequestBody AlarmHandleRequest request,
                                    @RequestHeader("X-User-Id") Long handlerId,
                                    @RequestHeader("X-User-Name") String handlerName) {
        alarmService.handleAlarm(request, handlerId, handlerName);
        return Result.success();
    }

    /**
     * 批量处理告警
     */
    @PostMapping("/batch-handle")
    public Result<Void> batchHandleAlarms(@RequestBody Map<String, Object> params,
                                          @RequestHeader("X-User-Id") Long handlerId,
                                          @RequestHeader("X-User-Name") String handlerName) {
        @SuppressWarnings("unchecked")
        List<Long> alarmIds = (List<Long>) params.get("alarmIds");
        String handleMethod = (String) params.get("handleMethod");
        String remark = (String) params.get("remark");

        alarmService.batchHandleAlarms(alarmIds, handleMethod, remark, handlerId, handlerName);
        return Result.success();
    }

    /**
     * 告警统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = alarmService.getStatistics();
        return Result.success(statistics);
    }
    @GetMapping("/stats")
    public Result<Map<String, Object>> getAlarmModuleStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("pending", 10);
        data.put("handled", 50);
        return Result.success(data);
    }

}
