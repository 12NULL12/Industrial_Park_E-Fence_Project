package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.dto.dispatch.InstructionCreateRequest;
import com.fence.dto.dispatch.InstructionDetailVO;
import com.fence.entity.CommandPayload;
import com.fence.entity.Instruction;
import com.fence.entity.Vehicle;
import com.fence.mapper.InstructionMapper;
import com.fence.mapper.TaskMapper;
import com.fence.mapper.VehicleMapper;
import com.fence.websocket.WebSocketWithOffline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DispatchService {

    @Autowired
    private InstructionMapper instructionMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private MqttPublishService mqttPublishService;
    @Autowired
    private VehicleMapper vehicleMapper;

    /**
     * 创建调度指令
     */
    @Transactional
    public Long createAndSendInstruction(InstructionCreateRequest request,
                                         Long operatorId, String operatorName) {
        Instruction instruction = new Instruction();
        BeanUtils.copyProperties(request, instruction);

        instruction.setInstructionNo(generateInstructionNo());

        if (request.getTaskId() != null) {
            var task = taskMapper.selectById(request.getTaskId());
            if (task != null) {
                instruction.setTaskNo(task.getTaskNo());
                if (instruction.getVehicleId() == null) {
                    instruction.setVehicleId(task.getVehicleId());
                    instruction.setVehiclePlate(task.getVehiclePlate());
                }
            }
        }

        instruction.setOperatorId(operatorId);
        instruction.setOperatorName(operatorName);
        instruction.setCreateTime(LocalDateTime.now());
        instruction.setUpdateTime(LocalDateTime.now());

        instructionMapper.insert(instruction);

        log.info("创建调度指令成功: instructionNo={}, type={}, operator={}",
                instruction.getInstructionNo(), request.getInstructionType(), operatorName);

        sendInstruction(instruction.getId());

        return instruction.getId();
    }

    /**
     * 下发指令（发送到设备）
     */
    @Transactional
    public void sendInstruction(Long id) {
        String DEVICE_ID="vehicle_00";
        Instruction instruction = instructionMapper.selectById(id);
        if (instruction == null) {
            throw new BusinessException("指令不存在");
        }

        if (!"PENDING".equals(instruction.getStatus())) {
            throw new BusinessException("指令状态不允许发送");
        }

        String commandId = "CMD" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        CommandPayload payload = new CommandPayload();
        payload.setCommandId(commandId);
        payload.setType(instruction.getInstructionType());
        payload.setVehicleId(String.valueOf(instruction.getVehicleId()));
        payload.setReason(instruction.getInstructionContent());
        payload.setOperatorId(instruction.getOperatorId() != null ? String.valueOf(instruction.getOperatorId()) : "SYSTEM");
        payload.setTimestamp(System.currentTimeMillis());

        String topic = "vehicle/" + DEVICE_ID + instruction.getVehicleId() + "/command";
        mqttPublishService.publish(topic, payload);

        instruction.setCommandId(commandId);
        instructionMapper.updateCommandId(id, commandId);
        instructionMapper.updateSent(id);

        log.info("下发指令成功: instructionId={}, commandId={}, vehicleId={}, type={}",
                id, commandId, instruction.getVehicleId(), instruction.getInstructionType());
    }

    @Transactional
    public void handleInstructionAck(Long vehicleId, String instructionType, String status, String message) {
        log.info("处理设备指令确认: vehicleId={}, instructionType={}, status={}",
                vehicleId, instructionType, status);

        Instruction latestInstruction = instructionMapper.selectLatestByVehicleAndType(vehicleId, instructionType);

        if (latestInstruction == null) {
            log.warn("未找到对应的指令记录: vehicleId={}, instructionType={}", vehicleId, instructionType);
            return;
        }

        if (!"SENT".equals(latestInstruction.getStatus())) {
            log.warn("指令状态不正确，无需确认: instructionId={}, status={}",
                    latestInstruction.getId(), latestInstruction.getStatus());
            return;
        }

        String newStatus = "COMPLETED";
        if ("received".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
            newStatus = "COMPLETED";
        } else if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
            newStatus = "FAILED";
        }

        if ("FAILED".equals(newStatus)) {
            instructionMapper.updateFailed(latestInstruction.getId(), message);
        } else {
            instructionMapper.updateCompleted(latestInstruction.getId(), message);
        }

        log.info("指令确认成功: instructionId={}, newStatus={}", latestInstruction.getId(), newStatus);

        pushInstructionStatusToFrontend(latestInstruction, newStatus, message);
    }

    /**
     * 推送指令状态更新到前端
     */
    private void pushInstructionStatusToFrontend(Instruction instruction, String newStatus, String message) {
        try {
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("type", "INSTRUCTION_STATUS_UPDATE");
            statusMessage.put("instructionId", instruction.getId());
            statusMessage.put("instructionNo", instruction.getInstructionNo());
            statusMessage.put("commandId", instruction.getCommandId());
            statusMessage.put("vehicleId", instruction.getVehicleId());
            statusMessage.put("vehiclePlate", instruction.getVehiclePlate());
            statusMessage.put("instructionType", instruction.getInstructionType());
            statusMessage.put("status", newStatus);
            statusMessage.put("message", message);
            statusMessage.put("updateTime", LocalDateTime.now().toString());
            statusMessage.put("serverTime", System.currentTimeMillis());

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(statusMessage);

            Set<String> targetUserIds = new HashSet<>();

            targetUserIds.add("dispatcher_1");
            log.debug("添加调度员到推送目标: userId=dispatcher_1");

            if (instruction.getOperatorId() != null) {
                String operatorUserId = "user_" + instruction.getOperatorId();
                targetUserIds.add(operatorUserId);
                log.debug("添加操作人到推送目标: userId={}", operatorUserId);
            }

            if (vehicleMapper != null) {
                try {
                    Vehicle vehicle = vehicleMapper.selectById(instruction.getVehicleId());
                    if (vehicle != null && vehicle.getDriverId() != null) {
                        String driverUserId = "driver_" + vehicle.getDriverId();
                        targetUserIds.add(driverUserId);
                        log.debug("添加司机到推送目标: userId={}, vehicleId={}",
                                driverUserId, instruction.getVehicleId());
                    }
                } catch (Exception e) {
                    log.error("查询车辆信息失败: vehicleId={}", instruction.getVehicleId(), e);
                }
            }

            for (String userId : targetUserIds) {
                try {
                    WebSocketWithOffline.sendToUser(userId, jsonMessage);
                    log.debug("推送指令状态给 {}: status={}, instructionId={}",
                            userId, newStatus, instruction.getId());
                } catch (Exception e) {
                    log.error("推送指令状态给单个用户失败: userId={}, instructionId={}",
                            userId, instruction.getId(), e);
                }
            }

            log.info("指令状态已推送到前端: instructionId={}, status={}, targets={}",
                    instruction.getId(), newStatus, targetUserIds.size());

        } catch (Exception e) {
            log.error("推送指令状态到前端失败: instructionId={}", instruction.getId(), e);
        }
    }



    /**
     * 确认指令接收（设备端回调）
     */
    @Transactional
    public void confirmReceived(String commandId) {
        Instruction instruction = instructionMapper.selectByCommandId(commandId);
        if (instruction == null) {
            log.warn("未找到对应的指令记录: commandId={}", commandId);
            return;
        }

        if (!"SENT".equals(instruction.getStatus())) {
            log.warn("指令状态不正确，无法确认接收: instructionId={}, status={}",
                    instruction.getId(), instruction.getStatus());
            return;
        }

        instructionMapper.updateReceived(instruction.getId());

        log.info("确认指令接收: commandId={}, instructionId={}", commandId, instruction.getId());
    }


    /**
     * 开始执行指令
     */
    @Transactional
    public void startExecute(String commandId) {
        Instruction instruction = instructionMapper.selectByCommandId(commandId);
        if (instruction == null) {
            log.warn("未找到对应的指令记录: commandId={}", commandId);
            return;
        }

        if (!"RECEIVED".equals(instruction.getStatus())) {
            log.warn("指令状态不正确，无法执行: instructionId={}, status={}",
                    instruction.getId(), instruction.getStatus());
            return;
        }

        instructionMapper.updateExecuting(instruction.getId());

        log.info("开始执行指令: commandId={}, instructionId={}", commandId, instruction.getId());
    }


    /**
     * 完成指令
     */
    @Transactional
    public void completeInstruction(String commandId, String feedback) {
        Instruction instruction = instructionMapper.selectByCommandId(commandId);
        if (instruction == null) {
            log.warn("未找到对应的指令记录: commandId={}", commandId);
            return;
        }

        if (!"EXECUTING".equals(instruction.getStatus())) {
            log.warn("指令状态不正确，无法完成: instructionId={}, status={}",
                    instruction.getId(), instruction.getStatus());
            return;
        }

        instructionMapper.updateCompleted(instruction.getId(), feedback);

        log.info("完成指令: commandId={}, instructionId={}, feedback={}",
                commandId, instruction.getId(), feedback);
    }

    /**
     * 指令执行失败
     */
    @Transactional
    public void failInstruction(String commandId, String failReason) {
        Instruction instruction = instructionMapper.selectByCommandId(commandId);
        if (instruction == null) {
            log.warn("未找到对应的指令记录: commandId={}", commandId);
            return;
        }

        instructionMapper.updateFailed(instruction.getId(), failReason);

        log.warn("指令执行失败: commandId={}, instructionId={}, reason={}",
                commandId, instruction.getId(), failReason);
    }

    /**
     * 查询指令详情
     */
    public InstructionDetailVO getInstructionDetail(Long id) {
        Instruction instruction = instructionMapper.selectById(id);
        if (instruction == null) {
            throw new BusinessException("指令不存在");
        }
        return convertToVO(instruction);
    }

    /**
     * 查询指令列表
     */
    public List<InstructionDetailVO> queryInstructions(int limit,
                                                             String status,
                                                             String instructionType,
                                                             Long taskId,
                                                             Long vehicleId,
                                                             LocalDateTime startTime,
                                                             LocalDateTime endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("instructionType", instructionType);
        params.put("taskId", taskId);
        params.put("vehicleId", vehicleId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("limit", limit);
        params.put("offset", 0);
        List<Instruction> instructions = instructionMapper.selectList(params);
        int total = instructionMapper.count(params);

        List<InstructionDetailVO> voList = instructions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 根据任务查询指令列表
     */
    public List<InstructionDetailVO> getInstructionsByTask(Long taskId) {
        List<Instruction> instructions = instructionMapper.selectByTaskId(taskId);
        return instructions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据车辆查询指令列表
     */
    public List<InstructionDetailVO> getInstructionsByVehicle(Long vehicleId) {
        List<Instruction> instructions = instructionMapper.selectByVehicleId(vehicleId);
        return instructions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量下发待发送指令（定时任务调用）
     */
    @Transactional
    public void batchSendPendingInstructions() {
        List<Instruction> pendingInstructions = instructionMapper.selectPendingInstructions();

        for (Instruction instruction : pendingInstructions) {
            try {
                sendInstruction(instruction.getId());
            } catch (Exception e) {
                log.error("下发指令失败: instructionId={}", instruction.getId(), e);
            }
        }

        if (!pendingInstructions.isEmpty()) {
            log.info("批量下发指令数量: {}", pendingInstructions.size());
        }
    }

    /**
     * 取消指令
     */
    @Transactional
    public void cancelInstruction(Long id) {
        Instruction instruction = instructionMapper.selectById(id);
        if (instruction == null) {
            throw new BusinessException("指令不存在");
        }

        if ("COMPLETED".equals(instruction.getStatus()) ||
                "EXECUTING".equals(instruction.getStatus())) {
            throw new BusinessException("指令已完成或执行中，无法取消");
        }

        instructionMapper.updateStatus(id, "CANCELLED");

        log.info("取消指令: instructionId={}", id);
    }
    /**
     * 紧急停车逻辑
     */
    @Transactional
    public void emergencyStop(Long vehicleId, Long userId) {
        Instruction instruction = new Instruction();
        instruction.setInstructionNo(generateInstructionNo());
        instruction.setVehicleId(vehicleId);
        instruction.setInstructionType("EMERGENCY_STOP");
        instruction.setInstructionContent("系统触发紧急停车指令");
        instruction.setStatus("PENDING");
        instruction.setOperatorId(userId);
        instruction.setSendTime(LocalDateTime.now());

        instructionMapper.insert(instruction);

        sendInstruction(instruction.getId());

        log.info("紧急停车指令已创建并发送: vehicleId={}, instructionId={}", vehicleId, instruction.getId());
    }


    // ==================== 私有辅助方法 ====================

    private String generateInstructionNo() {
        return "INS" + System.currentTimeMillis();
    }

    private InstructionDetailVO convertToVO(Instruction instruction) {
        InstructionDetailVO vo = new InstructionDetailVO();
        BeanUtils.copyProperties(instruction, vo);
        return vo;
    }
}
