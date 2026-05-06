package com.fence.controller;

import com.fence.common.PageResult;
import com.fence.common.Result;
import com.fence.dto.driver.DriverCreateRequest;
import com.fence.dto.driver.DriverDetailVO;
import com.fence.dto.driver.DriverUpdateRequest;
import com.fence.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/driver")
public class DriverController {

    @Autowired
    private DriverService driverService;

    /**
     * 创建司机
     */
    @PostMapping("/create")
    public Result<Long> createDriver(@Valid @RequestBody DriverCreateRequest request) {
        Long driverId = driverService.createDriver(request);
        return Result.success(driverId);
    }

    /**
     * 更新司机信息
     */
    @PutMapping("/update")
    public Result<Void> updateDriver(@Valid @RequestBody DriverUpdateRequest request) {
        driverService.updateDriver(request);
        return Result.success();
    }

    /**
     * 删除司机
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return Result.success();
    }

    /**
     * 查询司机详情
     */
    @GetMapping("/{id}")
    public Result<DriverDetailVO> getDriverDetail(@PathVariable Long id) {
        DriverDetailVO detail = driverService.getDriverDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询司机列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> queryDrivers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;

        List<DriverDetailVO> list = driverService.queryDrivers(
                maxLimit, status, name, phone
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 绑定车辆
     */
    @PostMapping("/{id}/bind")
    public Result<Void> bindVehicle(@PathVariable Long id,
                                    @RequestParam Long vehicleId) {
        driverService.bindVehicle(id, vehicleId);
        return Result.success();
    }

    /**
     * 解绑车辆
     */
    @PostMapping("/{id}/unbind-vehicle")
    public Result<Void> unbindVehicle(@PathVariable Long id) {
        driverService.unbindVehicle(id);
        return Result.success();
    }

    /**
     * 更新司机状态
     */
    @PostMapping("/{id}/status")
    public Result<Void> updateDriverStatus(@PathVariable Long id,
                                           @RequestParam String status) {
        driverService.updateDriverStatus(id, status);
        return Result.success();
    }

    /**
     * 查询可用司机列表
     */
    @GetMapping("/available")
    public Result<List<DriverDetailVO>> getAvailableDrivers() {
        List<DriverDetailVO> drivers = driverService.getAvailableDrivers();
        return Result.success(drivers);
    }

    /**
     * 根据手机号查询司机
     */
    @GetMapping("/by-phone/{phone}")
    public Result<DriverDetailVO> getDriverByPhone(@PathVariable String phone) {
        DriverDetailVO detail = driverService.getDriverByPhone(phone);
        return Result.success(detail);
    }
}
