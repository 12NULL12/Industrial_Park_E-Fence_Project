package com.fence.controller;

import com.fence.common.PageResult;
import com.fence.common.Result;
import com.fence.dto.task.TaskAssignRequest;
import com.fence.dto.task.TaskCreateRequest;
import com.fence.dto.task.TaskDetailVO;
import com.fence.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 创建任务
     */
    @PostMapping("/create")
    public Result<Long> createTask(@Valid @RequestBody TaskCreateRequest request) {
        Long taskId = taskService.createTask(request);
        return Result.success(taskId);
    }

    /**
     * 更新任务
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateTask(@PathVariable Long id,
                                   @Valid @RequestBody TaskCreateRequest request) {
        taskService.updateTask(id, request);
        return Result.success();
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{id}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable Long id) {
        TaskDetailVO detail = taskService.getTaskDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询任务列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> queryTasks(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;

        List<TaskDetailVO> list = taskService.queryTasks(
                maxLimit, status, vehicleId, driverId,
                taskName, startTime, endTime
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 分配任务
     */
    @PostMapping("/assign")
    public Result<Void> assignTask(@Valid @RequestBody TaskAssignRequest request) {
        taskService.assignTask(request);
        return Result.success();
    }

    /**
     * 开始任务
     */
    @PostMapping("/{id}/start")
    public Result<Void> startTask(@PathVariable Long id) {
        taskService.startTask(id);
        return Result.success();
    }

    /**
     * 完成任务
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeTask(@PathVariable Long id) {
        taskService.completeTask(id);
        return Result.success();
    }

    /**
     * 取消任务
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable Long id) {
        taskService.cancelTask(id);
        return Result.success();
    }

    /**
     * 根据任务编号查询
     */
    @GetMapping("/by-no/{taskNo}")
    public Result<TaskDetailVO> getTaskByTaskNo(@PathVariable String taskNo) {
        TaskDetailVO detail = taskService.getTaskByTaskNo(taskNo);
        return Result.success(detail);
    }

    /**
     * 检查超时任务（定时任务调用）
     */
    @PostMapping("/check-timeout")
    public Result<Void> checkTimeoutTasks() {
        taskService.checkTimeoutTasks();
        return Result.success();
    }
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = taskService.getStatistics();
        return Result.success(statistics);
    }
}
