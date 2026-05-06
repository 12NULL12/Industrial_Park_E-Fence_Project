package com.fence.service;

import com.fence.dto.warehouse.WarehouseCreateRequest;
import com.fence.dto.warehouse.WarehouseDetailVO;
import com.fence.entity.Warehouse;
import com.fence.entity.WarehouseAdmin;
import com.fence.mapper.WarehouseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
    @Autowired
    private WarehouseMapper warehouseMapper;

    /**
     * 获取仓库列表
     */
    public List<WarehouseDetailVO> getWarehouseList() {
        List<Warehouse> warehouses = warehouseMapper.selectAllWithAdmin();

        return warehouses.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取仓库详情
     */
    public WarehouseDetailVO getWarehouseDetail(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }
        return convertToVO(warehouse);
    }

    /**
     * 新增仓库
     */
    @Transactional
    public Long createWarehouse(WarehouseCreateRequest request) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setLongitude(request.getLongitude());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setAdminId(request.getAdminId());
        warehouseMapper.insert(warehouse);
        return warehouse.getId();
    }

    /**
     * 修改仓库
     */
    @Transactional
    public void updateWarehouse(Long id, WarehouseCreateRequest request) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }

        warehouse.setWarehouseName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setLongitude(request.getLongitude());
        warehouse.setLatitude(request.getLatitude());
        warehouse.setAdminId(request.getAdminId());
        warehouseMapper.updateById(warehouse);
    }

    /**
     * 删除仓库
     */
    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new RuntimeException("仓库不存在");
        }
        warehouseMapper.deleteById(id);
    }

    /**
     * 转换为VO
     */
    private WarehouseDetailVO convertToVO(Warehouse warehouse) {
        WarehouseDetailVO vo = new WarehouseDetailVO();
        vo.setId(warehouse.getId());
        vo.setName(warehouse.getWarehouseName());
        vo.setAddress(warehouse.getAddress());
        vo.setLongitude(warehouse.getLongitude());
        vo.setLatitude(warehouse.getLatitude());
        vo.setAdminName(getAdminNameById(warehouse.getAdminId()));
        return vo;
    }

    /**
     * 根据adminId获取管理员名称（这里简化处理，实际应该查询用户表）
     */
    private String getAdminNameById(Long adminId) {
        if (adminId == null) {
            return null;
        }
        WarehouseAdmin admin = warehouseMapper.selectAdminById(adminId);
        return admin != null ? admin.getAdminName() : null;
    }
}
