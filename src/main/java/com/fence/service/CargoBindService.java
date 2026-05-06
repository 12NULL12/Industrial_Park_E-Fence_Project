package com.fence.service;

import com.fence.dto.cargo.CargoBindRequest;
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

@Service
public class CargoBindService {

    @Autowired
    private CargoMapper cargoMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskCargoMapper taskCargoMapper;

    /**
     * 司机绑定货物（通过货物编码）
     */
    @Transactional
    public void bindCargoByCode(CargoBindRequest request) {
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        Cargo cargo = getCargoByCode(request.getCargoCode());
        if (cargo == null) {
            throw new RuntimeException("货物不存在，请检查货物编码");
        }

        if ("inTransit".equals(cargo.getStatus()) || "delivered".equals(cargo.getStatus())) {
            throw new RuntimeException("货物已在运输中或已送达，无法绑定");
        }

        TaskCargo taskCargo = new TaskCargo();
        taskCargo.setTaskId(request.getTaskId());
        taskCargo.setCargoId(cargo.getId());
        taskCargo.setCargoName(cargo.getCargoName());
        taskCargo.setWeight(cargo.getWeight());
        taskCargo.setVolume(null);
        taskCargo.setQuantity(cargo.getQuantity());
        taskCargo.setUnit(cargo.getUnit());
        taskCargo.setCreateTime(LocalDateTime.now());
        taskCargoMapper.insert(taskCargo);

        cargo.setTaskId(request.getTaskId());
        cargo.setStatus("inTransit");
        cargo.setUpdatedAt(LocalDateTime.now());
        cargoMapper.updateById(cargo);
    }

    /**
     * 根据货物编码查询货物
     */
    private Cargo getCargoByCode(String cargoCode) {
        return cargoMapper.selectByCargoCode(cargoCode);
    }
}

