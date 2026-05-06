package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.warehouse.WarehouseCreateRequest;
import com.fence.dto.warehouse.WarehouseDetailVO;
import com.fence.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseService warehouseService;

    /**
     * 13.1 获取仓库列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getList() {
        List<WarehouseDetailVO> list = warehouseService.getWarehouseList();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());

        return Result.success(data);
    }

    /**
     * 13.2 获取仓库详情
     */
    @GetMapping("/{id}")
    public Result<WarehouseDetailVO> getDetail(@PathVariable Long id) {
        WarehouseDetailVO detail = warehouseService.getWarehouseDetail(id);
        return Result.success(detail);
    }

    /**
     * 13.3 新增仓库
     */
    @PostMapping
    public Result<Long> create(@RequestBody WarehouseCreateRequest request) {
        Long warehouseId = warehouseService.createWarehouse(request);
        return Result.success(warehouseId);
    }

    /**
     * 13.4 修改仓库
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody WarehouseCreateRequest request) {
        warehouseService.updateWarehouse(id, request);
        return Result.success();
    }

    /**
     * 13.5 删除仓库
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return Result.success();
    }
}

