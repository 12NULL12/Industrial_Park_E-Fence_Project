package com.fence.service;

import com.fence.dto.cargo.CargoCreateRequest;
import com.fence.dto.cargo.CargoDetailVO;
import com.fence.entity.Cargo;
import com.fence.entity.Warehouse;
import com.fence.mapper.CargoMapper;
import com.fence.mapper.WarehouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CargoService {

    @Autowired
    private CargoMapper cargoMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    /**
     * 获取货物列表
     */
    public List<CargoDetailVO> getCargoList() {
        List<Cargo> cargos = cargoMapper.selectAllWithWarehouse();
        return cargos.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取货物详情
     */
    public CargoDetailVO getCargoDetail(Long id) {
        Cargo cargo = cargoMapper.selectByIdWithWarehouse(id);
        if (cargo == null) {
            throw new RuntimeException("货物不存在");
        }
        return convertToVO(cargo);
    }

    /**
     * 新增货物
     */
    @Transactional
    public Long createCargo(CargoCreateRequest request) {
        Cargo cargo = new Cargo();
        cargo.setCargoName(request.getName());
        cargo.setCargoCode(request.getCode());
        cargo.setCargoType(request.getType());
        cargo.setWeight(request.getWeight());
        cargo.setUnit(request.getUnit());
        cargo.setQuantity(request.getQuantity());
        cargo.setStatus("pending");
        cargo.setSourceWarehouseId(request.getSourceWarehouseId());
        cargo.setDestWarehouseId(request.getDestWarehouseId());
        cargo.setCreatedAt(LocalDateTime.now());
        cargo.setUpdatedAt(LocalDateTime.now());
        cargoMapper.insert(cargo);
        return cargo.getId();
    }

    /**
     * 修改货物
     */
    @Transactional
    public void updateCargo(Long id, CargoCreateRequest request) {
        Cargo cargo = cargoMapper.selectById(id);
        if (cargo == null) {
            throw new RuntimeException("货物不存在");
        }

        cargo.setCargoName(request.getName());
        cargo.setCargoCode(request.getCode());
        cargo.setCargoType(request.getType());
        cargo.setWeight(request.getWeight());
        cargo.setUnit(request.getUnit());
        cargo.setQuantity(request.getQuantity());
        cargo.setSourceWarehouseId(request.getSourceWarehouseId());
        cargo.setDestWarehouseId(request.getDestWarehouseId());
        cargo.setUpdatedAt(LocalDateTime.now());
        cargoMapper.updateById(cargo);
    }

    /**
     * 标记货物已送达
     */
    @Transactional
    public void markAsDelivered(Long id) {
        Cargo cargo = cargoMapper.selectById(id);
        if (cargo == null) {
            throw new RuntimeException("货物不存在");
        }

        cargo.setStatus("delivered");
        cargo.setUpdatedAt(LocalDateTime.now());
        cargoMapper.updateById(cargo);
    }

    /**
     * 转换为VO
     */
    private CargoDetailVO convertToVO(Cargo cargo) {
        CargoDetailVO vo = new CargoDetailVO();
        vo.setId(cargo.getId());
        vo.setName(cargo.getCargoName());
        vo.setCode(cargo.getCargoCode());
        vo.setType(cargo.getCargoType());
        vo.setWeight(cargo.getWeight());
        vo.setUnit(cargo.getUnit());
        vo.setQuantity(cargo.getQuantity());
        vo.setStatus(cargo.getStatus());
        vo.setSourceWarehouse(getWarehouseNameById(cargo.getSourceWarehouseId()));
        vo.setDestWarehouse(getWarehouseNameById(cargo.getDestWarehouseId()));
        return vo;
    }

    /**
     * 根据仓库ID获取仓库名称
     */
    private String getWarehouseNameById(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null ? warehouse.getWarehouseName() : null;
    }
}
