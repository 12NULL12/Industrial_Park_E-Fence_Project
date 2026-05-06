package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.vehicle.VehicleCreateRequest;
import com.fence.dto.vehicle.VehicleDetailVO;
import com.fence.dto.vehicle.VehicleLocationVO;
import com.fence.dto.vehicle.VehicleUpdateRequest;
import com.fence.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    // ==================== 车辆管理接口 ====================

    /**
     * 创建车辆
     */
    @PostMapping("")
    public Result<Long> createVehicle(@Valid @RequestBody VehicleCreateRequest request) {
        Long vehicleId = vehicleService.createVehicle(request);
        return Result.success(vehicleId);
    }

    /**
     * 更新车辆信息
     */
    @PutMapping("/{id}")
    public Result<Void> updateVehicle(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        vehicleService.updateVehicle(id, request);
        return Result.success();
    }

    /**
     * 删除车辆
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return Result.success();
    }

    /**
     * 查询车辆详情
     */
    @GetMapping("/{id}")
    public Result<VehicleDetailVO> getVehicleDetail(@PathVariable Long id) {
        VehicleDetailVO detail = vehicleService.getVehicleDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询车辆列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> queryVehicles(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String vehicleType) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;

        List<VehicleDetailVO> list = vehicleService.queryVehicles(
                maxLimit, status, plateNumber, vehicleType
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 根据车牌号查询车辆
     */
    @GetMapping("/by-plate/{plateNumber}")
    public Result<VehicleDetailVO> getVehicleByPlateNumber(@PathVariable String plateNumber) {
        VehicleDetailVO detail = vehicleService.getVehicleByPlateNumber(plateNumber);
        return Result.success(detail);
    }

    /**
     * 更新车辆状态
     */
    @PostMapping("/{id}/status")
    public Result<Void> updateVehicleStatus(@PathVariable Long id,
                                            @RequestParam String status) {
        vehicleService.updateVehicleStatus(id, status);
        return Result.success();
    }

    /**
     * 绑定司机
     */
    @PostMapping("/{id}/bind-driver")
    public Result<Void> bindDriver(@PathVariable Long id,
                                   @RequestParam Long driverId,
                                   @RequestParam String driverName) {
        vehicleService.bindDriver(id, driverId, driverName);
        return Result.success();
    }

    /**
     * 解绑司机
     */
    @PostMapping("/{id}/unbind-driver")
    public Result<Void> unbindDriver(@PathVariable Long id) {
        vehicleService.unbindDriver(id);
        return Result.success();
    }

    /**
     * 更新车辆位置
     */
    @PostMapping("/{id}/position")
    public Result<Void> updateVehiclePosition(@PathVariable Long id,
                                              @RequestParam Double longitude,
                                              @RequestParam Double latitude) {
        vehicleService.updateVehiclePosition(id, longitude, latitude);
        return Result.success();
    }

    /**
     * 获取在线车辆数量
     */
    @GetMapping("/online-count")
    public Result<Integer> getOnlineVehicleCount() {
        int count = vehicleService.getOnlineVehicleCount();
        return Result.success(count);
    }

    /**
     * 获取空闲车辆列表
     */
    @GetMapping("/idle-list")
    public Result<List<VehicleDetailVO>> getIdleVehicles() {
        List<VehicleDetailVO> vehicles = vehicleService.getIdleVehicles();
        return Result.success(vehicles);
    }

    /**
     * 获取所有车辆实时位置
     */
    @GetMapping("/location/all")
    public Result<List<VehicleLocationVO>> getAllLocations() {
        List<VehicleLocationVO> locations = vehicleService.getAllVehicleLocations();
        return Result.success(locations);
    }

    // ==================== 远程控制接口 ====================

}
