 package com.fence.service;

import com.fence.entity.Cargo;
import com.fence.entity.Task;
import com.fence.entity.TaskCargo;
import com.fence.mapper.CargoMapper;
import com.fence.mapper.TaskCargoMapper;
import com.fence.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiveService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private CargoMapper cargoMapper;

    @Autowired
    private TaskCargoMapper taskCargoMapper;

    /**
     * 验证货物信息
     */
    public Map<String, Object> verifyCargo(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        List<TaskCargo> taskCargos = taskCargoMapper.selectByTaskId(taskId);
        if (taskCargos == null || taskCargos.isEmpty()) {
            throw new RuntimeException("该任务下没有货物");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("taskNo", task.getTaskNo());
        result.put("cargos", taskCargos);
        result.put("totalQuantity", taskCargos.stream().mapToInt(tc -> tc.getQuantity() != null ? tc.getQuantity() : 0).sum());

        return result;
    }

    /**
     * 确认收货
     */
    @Transactional
    public void receiveCargo(Long taskId, String checkResult) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        List<TaskCargo> taskCargos = taskCargoMapper.selectByTaskId(taskId);
        if (taskCargos == null || taskCargos.isEmpty()) {
            throw new RuntimeException("该任务下没有货物");
        }

        for (TaskCargo tc : taskCargos) {
            Cargo cargo = cargoMapper.selectById(tc.getCargoId());
            if (cargo != null) {
                cargo.setStatus("已收货");
                cargo.setUpdatedAt(LocalDateTime.now());
                cargoMapper.updateById(cargo);
            }
        }

        task.setStatus("COMPLETED");
        task.setActualEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

    }


}
