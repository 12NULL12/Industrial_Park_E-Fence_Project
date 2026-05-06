package com.fence.controller;

import com.fence.common.PageResult;
import com.fence.common.Result;
import com.fence.dto.device.DeviceCreateRequest;
import com.fence.dto.device.DeviceDetailVO;
import com.fence.dto.device.DeviceUpdateRequest;
import com.fence.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    /**
     * 创建设备
     */
    @PostMapping("/create")
    public Result<Long> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        Long deviceId = deviceService.createDevice(request);
        return Result.success(deviceId);
    }

    /**
     * 更新设备信息
     */
    @PostMapping("/update")
    public Result<Void> updateDevice(@Valid @RequestBody DeviceUpdateRequest request) {
        deviceService.updateDevice(request);
        return Result.success();
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success();
    }

    /**
     * 查询设备详情
     */
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> getDeviceDetail(@PathVariable Long id) {
        DeviceDetailVO detail = deviceService.getDeviceDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询设备列表
     */
    @GetMapping("/list")
    public  Result<Map<String, Object>> queryDevices(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String deviceNo,
            @RequestParam(required = false) Long vehicleId) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;


        List<DeviceDetailVO> list = deviceService.queryDevices(
                 maxLimit,status, deviceType, deviceNo, vehicleId
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 绑定车辆
     */
    @PostMapping("/{id}/bind-vehicle")
    public Result<Void> bindVehicle(@PathVariable Long id,
                                    @RequestParam Long vehicleId,
                                    @RequestParam String vehiclePlate) {
        deviceService.bindVehicle(id, vehicleId, vehiclePlate);
        return Result.success();
    }

    /**
     * 解绑车辆
     */
    @PostMapping("/{id}/unbind-vehicle")
    public Result<Void> unbindVehicle(@PathVariable Long id) {
        deviceService.unbindVehicle(id);
        return Result.success();
    }

    /**
     * 标记设备故障
     */
    @PostMapping("/{id}/mark-fault")
    public Result<Void> markDeviceFault(@PathVariable Long id,
                                        @RequestParam String reason) {
        deviceService.markDeviceFault(id, reason);
        return Result.success();
    }

    /**
     * 恢复设备正常
     */
    @PostMapping("/{id}/recover")
    public Result<Void> recoverDevice(@PathVariable Long id) {
        deviceService.recoverDevice(id);
        return Result.success();
    }

    /**
     * 根据设备编号查询
     */
    @GetMapping("/by-no/{deviceNo}")
    public Result<DeviceDetailVO> getDeviceByDeviceNo(@PathVariable String deviceNo) {
        DeviceDetailVO detail = deviceService.getDeviceByDeviceNo(deviceNo);
        return Result.success(detail);
    }

    /**
     * 检查离线设备（定时任务）
     */
    @PostMapping("/check-offline")
    public Result<Void> checkOfflineDevices() {
        deviceService.checkOfflineDevices();
        return Result.success();
    }
}
