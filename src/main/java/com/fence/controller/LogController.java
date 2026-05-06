package com.fence.controller;

import com.fence.common.PageResult;
import com.fence.common.Result;
import com.fence.dto.log.LogDetailVO;
import com.fence.dto.log.LogQueryRequest;
import com.fence.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * 1. 获取操作日志 (对应前端: GET /api/log/operation)
     */
    @GetMapping("/operation")
    public Result<Map<String, Object>> queryOperationLogs(@RequestParam(required = false) Integer limit) {
        return getLogResult("OPERATION", limit);
    }

    /**
     * 2. 获取登录日志 (对应前端: GET /api/log/login)
     */
    @GetMapping("/login")
    public Result<Map<String, Object>> queryLoginLogs(@RequestParam(required = false) Integer limit) {
        return getLogResult("LOGIN", limit);
    }

    /**
     * 3. 获取调度日志 (对应前端: GET /api/log/dispatch)
     */
    @GetMapping("/dispatch")
    public Result<Map<String, Object>> queryDispatchLogs(@RequestParam(required = false) Integer limit) {
        return getLogResult("DISPATCH", limit);
    }

    /**
     * 通用查询逻辑（适配前端分页：返回 list 和 total）
     */
    private Result<Map<String, Object>> getLogResult(String logType, Integer limit) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;
        // 调用 Service 根据类型查询
        List<LogDetailVO> list = logService.queryLogsByType(logType, maxLimit);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());

        return Result.success(data);
    }

    /**
     * 查询日志详情
     */
    @GetMapping("/{id}")
    public Result<LogDetailVO> getLogDetail(@PathVariable Long id) {
        LogDetailVO detail = logService.getLogDetail(id);
        return Result.success(detail);
    }

    /**
     * 清理旧日志
     */
    @PostMapping("/clean")
    public Result<Integer> cleanOldLogs(@RequestParam(defaultValue = "90") int days) {
        int count = logService.cleanOldLogs(days);
        return Result.success(count);
    }

    /**
     * 获取日志统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getLogStatistics() {
        Map<String, Object> stats = logService.getLogStatistics();
        return Result.success(stats);
    }
}
