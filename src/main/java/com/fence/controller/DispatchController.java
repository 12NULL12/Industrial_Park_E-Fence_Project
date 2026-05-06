package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.dispatch.InstructionCreateRequest;
import com.fence.dto.dispatch.InstructionDetailVO;
import com.fence.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dispatch")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    /**
     * 创建调度指令并下发
     */
    @PostMapping("/create")
    public Result<Long> createInstruction(@Valid @RequestBody InstructionCreateRequest request,
                                          @RequestHeader("X-User-Id") Long operatorId,
                                          @RequestHeader("X-User-Name") String operatorName) {
        Long instructionId = dispatchService.createAndSendInstruction(request, operatorId, operatorName);
        return Result.success(instructionId);
    }


    /**
     * 确认指令接收（设备端回调）
     */
    @PostMapping("/{commandId}/confirm-received")
    public Result<Void> confirmReceived(@PathVariable String commandId) {
        dispatchService.confirmReceived(commandId);
        return Result.success();
    }

    @PostMapping("/{commandId}/start-execute")
    public Result<Void> startExecute(@PathVariable String commandId) {
        dispatchService.startExecute(commandId);
        return Result.success();
    }

    /**
     * 完成指令
     */
    @PostMapping("/{commandId}/complete")
    public Result<Void> completeInstruction(@PathVariable String commandId,
                                            @RequestParam(required = false) String feedback) {
        dispatchService.completeInstruction(commandId, feedback);
        return Result.success();
    }

    /**
     * 指令执行失败
     */
    @PostMapping("/{commandId}/fail")
    public Result<Void> failInstruction(@PathVariable String commandId,
                                        @RequestParam String failReason) {
        dispatchService.failInstruction(commandId, failReason);
        return Result.success();
    }

    /**
     * 取消指令
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelInstruction(@PathVariable Long id) {
        dispatchService.cancelInstruction(id);
        return Result.success();
    }

    /**
     * 查询指令详情
     */
    @GetMapping("/{id}")
    public Result<InstructionDetailVO> getInstructionDetail(@PathVariable Long id) {
        InstructionDetailVO detail = dispatchService.getInstructionDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询指令列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>>queryInstructions(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String instructionType,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;

        List<InstructionDetailVO> list = dispatchService.queryInstructions(
                maxLimit, status, instructionType, taskId,
                vehicleId, startTime, endTime
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 根据任务查询指令
     */
    @GetMapping("/by-task/{taskId}")
    public Result<List<InstructionDetailVO>> getInstructionsByTask(@PathVariable Long taskId) {
        List<InstructionDetailVO> instructions = dispatchService.getInstructionsByTask(taskId);
        return Result.success(instructions);
    }

    /**
     * 根据车辆查询指令
     */
    @GetMapping("/by-vehicle/{vehicleId}")
    public Result<List<InstructionDetailVO>> getInstructionsByVehicle(@PathVariable Long vehicleId) {
        List<InstructionDetailVO> instructions = dispatchService.getInstructionsByVehicle(vehicleId);
        return Result.success(instructions);
    }

    /**
     * 批量下发待发送指令（定时任务）
     */
    @PostMapping("/batch-send")
    public Result<Void> batchSendPendingInstructions() {
        dispatchService.batchSendPendingInstructions();
        return Result.success();
    }
    /**
     * 紧急停车 (对应前端: POST /api/dispatch/emergency-stop)
     */
    @PostMapping("/emergency-stop")
    public Result<Void> emergencyStop(@RequestBody Map<String, Long> params,
                                      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Long vehicleId = params.get("vehicleId");
        dispatchService.emergencyStop(vehicleId, userId);
        return Result.success();
    }
}
