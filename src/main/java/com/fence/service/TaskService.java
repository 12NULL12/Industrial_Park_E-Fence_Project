package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.common.PageResult;
import com.fence.dto.task.TaskAssignRequest;
import com.fence.dto.task.TaskCreateRequest;
import com.fence.dto.task.TaskDetailVO;
import com.fence.entity.Task;
import com.fence.entity.TaskCargo;
import com.fence.entity.Vehicle;
import com.fence.mapper.TaskMapper;
import com.fence.mapper.VehicleMapper;
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
public class TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private VehicleMapper vehicleMapper;

    /**
     * 创建任务
     */
    @Transactional
    public Long createTask(TaskCreateRequest request) {
        Task task = new Task();
        BeanUtils.copyProperties(request, task);

        // 生成任务编号
        task.setTaskNo(generateTaskNo());
        task.setStatus("PENDING");
        if (task.getPriority() == null) {
            task.setPriority(2);
        }
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        taskMapper.insert(task);

        // 保存任务货物关联
        if (request.getCargos() != null && !request.getCargos().isEmpty()) {
            List<TaskCargo> cargos = new ArrayList<>();
            for (TaskCreateRequest.CargoItem item : request.getCargos()) {
                TaskCargo taskCargo = new TaskCargo();
                taskCargo.setTaskId(task.getId());
                taskCargo.setCargoId(item.getCargoId());
                taskCargo.setCargoName(item.getCargoName());
                taskCargo.setWeight(item.getWeight());
                taskCargo.setVolume(item.getVolume());
                taskCargo.setQuantity(item.getQuantity());
                taskCargo.setUnit(item.getUnit());
                taskCargo.setCreateTime(LocalDateTime.now());
                cargos.add(taskCargo);
            }
            taskMapper.batchInsertTaskCargos(cargos);
        }

        log.info("创建任务成功: taskNo={}, id={}", task.getTaskNo(), task.getId());
        return task.getId();
    }

    /**
     * 更新任务信息
     */
    @Transactional
    public void updateTask(Long id, TaskCreateRequest request) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 只有待分配的任务可以修改
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("任务已分配或执行中，无法修改");
        }

        BeanUtils.copyProperties(request, task);
        task.setId(id);
        task.setUpdateTime(LocalDateTime.now());

        taskMapper.update(task);

        // 更新货物关联
        if (request.getCargos() != null) {
            taskMapper.deleteCargosByTaskId(id);

            if (!request.getCargos().isEmpty()) {
                List<TaskCargo> cargos = new ArrayList<>();
                for (TaskCreateRequest.CargoItem item : request.getCargos()) {
                    TaskCargo taskCargo = new TaskCargo();
                    taskCargo.setTaskId(id);
                    taskCargo.setCargoId(item.getCargoId());
                    taskCargo.setCargoName(item.getCargoName());
                    taskCargo.setWeight(item.getWeight());
                    taskCargo.setVolume(item.getVolume());
                    taskCargo.setQuantity(item.getQuantity());
                    taskCargo.setUnit(item.getUnit());
                    taskCargo.setCreateTime(LocalDateTime.now());
                    cargos.add(taskCargo);
                }
                taskMapper.batchInsertTaskCargos(cargos);
            }
        }

        log.info("更新任务成功: id={}", id);
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 只有待分配的任务可以删除
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("任务已分配或执行中，无法删除");
        }

        taskMapper.deleteCargosByTaskId(id);
        taskMapper.deleteById(id);

        log.info("删除任务成功: id={}", id);
    }

    /**
     * 查询任务详情
     */
    public TaskDetailVO getTaskDetail(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        TaskDetailVO vo = convertToVO(task);

        // 查询任务货物
        List<TaskCargo> cargos = taskMapper.selectCargosByTaskId(id);
        List<TaskDetailVO.CargoVO> cargoVOs = cargos.stream()
                .map(c -> {
                    TaskDetailVO.CargoVO cargoVO = new TaskDetailVO.CargoVO();
                    cargoVO.setCargoId(c.getCargoId());
                    cargoVO.setCargoName(c.getCargoName());
                    cargoVO.setWeight(c.getWeight());
                    cargoVO.setVolume(c.getVolume());
                    cargoVO.setQuantity(c.getQuantity());
                    cargoVO.setUnit(c.getUnit());
                    return cargoVO;
                })
                .collect(Collectors.toList());
        vo.setCargos(cargoVOs);

        return vo;
    }

    /**
     * 查询任务列表
     */
    public List<TaskDetailVO> queryTasks(int limit,
                                               String status,
                                               Long vehicleId,
                                               Long driverId,
                                               String taskName,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("vehicleId", vehicleId);
        params.put("driverId", driverId);
        params.put("taskName", taskName);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("limit", limit);
        params.put("offset", 0);
        List<Task> tasks = taskMapper.selectList(params);
        int total = taskMapper.count(params);

        List<TaskDetailVO> voList = tasks.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 分配任务（关联车辆和司机）
     */
    @Transactional
    public void assignTask(TaskAssignRequest request) {
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("任务状态不允许分配");
        }

        Vehicle vehicle = vehicleMapper.selectById(request.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        String vehiclePlate = request.getVehiclePlate() != null ? request.getVehiclePlate() : vehicle.getPlateNumber();
        Long driverId = request.getDriverId() != null ? request.getDriverId() : vehicle.getDriverId();
        String driverName = request.getDriverName() != null ? request.getDriverName() : vehicle.getDriverName();

        taskMapper.assignVehicleAndDriver(
                request.getTaskId(),
                request.getVehicleId(),
                vehiclePlate,
                driverId,
                driverName
        );

        vehicle.setStatus("RUNNING");
        vehicle.setUpdateTime(LocalDateTime.now());
        vehicleMapper.update(vehicle);

        log.info("分配任务成功: taskId={}, vehicleId={}, vehiclePlate={}, driverId={}, driverName={}, vehicleStatus=RUNNING",
                request.getTaskId(), request.getVehicleId(), vehiclePlate, driverId, driverName);
    }

    /**
     * 开始任务
     */
    @Transactional
    public void startTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if (!"ASSIGNED".equals(task.getStatus())) {
            throw new BusinessException("任务未分配，无法开始");
        }

        taskMapper.updateActualStartTime(id);

        log.info("开始任务: id={}", id);
    }

    /**
     * 完成任务
     */
    @Transactional
    public void completeTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new BusinessException("任务未开始，无法完成");
        }

        taskMapper.updateActualEndTime(id);

        if (task.getVehicleId() != null) {
            Vehicle vehicle = vehicleMapper.selectById(task.getVehicleId());
            if (vehicle != null) {
                vehicle.setStatus("IDLE");
                vehicle.setUpdateTime(LocalDateTime.now());
                vehicleMapper.update(vehicle);
                log.info("任务完成，车辆状态改为空闲: vehicleId={}", task.getVehicleId());
            }
        }

        log.info("完成任务: id={}", id);
    }

    /**
     * 取消任务
     */
    @Transactional
    public void cancelTask(Long id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if ("COMPLETED".equals(task.getStatus())) {
            throw new BusinessException("已完成的任务无法取消");
        }

        taskMapper.updateStatus(id, "CANCELLED");

        if (task.getVehicleId() != null) {
            Vehicle vehicle = vehicleMapper.selectById(task.getVehicleId());
            if (vehicle != null) {
                vehicle.setStatus("IDLE");
                vehicle.setUpdateTime(LocalDateTime.now());
                vehicleMapper.update(vehicle);
                log.info("任务取消，车辆状态改为空闲: vehicleId={}", task.getVehicleId());
            }
        }

        log.info("取消任务: id={}", id);
    }

    /**
     * 检查并处理超时任务
     */
    @Transactional
    public void checkTimeoutTasks() {
        List<Task> timeoutTasks = taskMapper.selectTimeoutTasks();

        for (Task task : timeoutTasks) {
            taskMapper.updateStatus(task.getId(), "TIMEOUT");
            log.warn("任务超时: taskId={}, taskNo={}", task.getId(), task.getTaskNo());

            // TODO: 触发超时告警
            // alarmService.generateAlarm(...)
        }

        if (!timeoutTasks.isEmpty()) {
            log.info("处理超时任务数量: {}", timeoutTasks.size());
        }
    }

    /**
     * 根据任务编号查询任务
     */
    public TaskDetailVO getTaskByTaskNo(String taskNo) {
        Task task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return convertToVO(task);
    }
    /**
     * 获取任务统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", taskMapper.countByStatus(null));

        Map<String, Object> statusStats = taskMapper.statisticsByStatus();
        result.put("pending", statusStats.get("pending"));
        result.put("assigned", statusStats.get("assigned"));
        result.put("inProgress", statusStats.get("inProgress"));
        result.put("completed", statusStats.get("completed"));
        result.put("cancelled", statusStats.get("cancelled"));
        result.put("timeout", statusStats.get("timeout"));

        return result;
    }



    // ==================== 私有辅助方法 ====================

    private String generateTaskNo() {
        return "TSK" + System.currentTimeMillis();
    }

    private TaskDetailVO convertToVO(Task task) {
        TaskDetailVO vo = new TaskDetailVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
