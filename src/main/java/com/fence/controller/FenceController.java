package com.fence.controller;

import com.fence.common.PageResult;
import com.fence.common.Result;
import com.fence.dto.fence.FenceCreateRequest;
import com.fence.dto.fence.FenceDetailVO;
import com.fence.service.FenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fence")
public class FenceController {

    @Autowired
    private FenceService fenceService;

    /**
     * 创建围栏
     */
    @PostMapping("")
    public Result<Long> createFence(@Valid @RequestBody FenceCreateRequest request) {
        Long fenceId = fenceService.createFence(request);
        return Result.success(fenceId);
    }

    /**
     * 更新围栏
     */
    @PutMapping("/update/{id}")
    public Result<Void> updateFence(@PathVariable Long id,
                                    @Valid @RequestBody FenceCreateRequest request) {
        fenceService.updateFence(id, request);
        return Result.success();
    }

    /**
     * 删除围栏
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFence(@PathVariable Long id) {
        fenceService.deleteFence(id);
        return Result.success();
    }

    /**
     * 查询围栏详情
     */
    @GetMapping("/{id}")
    public Result<FenceDetailVO> getFenceDetail(@PathVariable Long id) {
        FenceDetailVO detail = fenceService.getFenceDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询围栏列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> queryFences(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String fenceType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String fenceName) {
        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 1000) : 100;
        List<FenceDetailVO> list = fenceService.queryFences(
                maxLimit, fenceType, enabled, fenceName
        );
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }

    /**
     * 绑定车辆到围栏
     */
    @PostMapping("/{fenceId}/bind")
    public Result<Void> bindVehicles(@PathVariable Long fenceId, @RequestBody Map<String, List<Long>> request) {
        List<Long> vehicleIds = request.get("vehicleIds");
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Result.error("请选择要绑定的车辆");
        }
        fenceService.bindVehicles(fenceId, vehicleIds);
        return Result.success();
    }

    /**
     * 解绑车辆
     */
    @PostMapping("/{id}/unbind-vehicle")
    public Result<Void> unbindVehicle(@PathVariable Long id,
                                      @RequestParam Long vehicleId) {
        fenceService.unbindVehicleFromFence(id, vehicleId);
        return Result.success();
    }

    /**
     * 检查车辆是否在围栏内（测试用）
     */
    @GetMapping("/check-position")
    public Result<Boolean> checkVehicleInFence(
            @RequestParam Long vehicleId,
            @RequestParam Long fenceId,
            @RequestParam Double longitude,
            @RequestParam Double latitude) {

        boolean inside = fenceService.checkVehicleInFence(
                vehicleId, fenceId, longitude, latitude
        );
        return Result.success(inside);
    }

    /**
     * 检查车辆是否触发围栏告警
     */
    @GetMapping("/check-alarm")
    public Result<Map<String, Object>> checkFenceAlarm(
            @RequestParam Long vehicleId,
            @RequestParam Double longitude,
            @RequestParam Double latitude) {

        Map<String, Object> result = fenceService.checkFenceAlarm(
                vehicleId, longitude, latitude
        );
        return Result.success(result);
    }

    /**
     * 根据司机ID获取其绑定车辆的围栏列表
     */
    @GetMapping("/driver/{driverId}")
    public Result<List<FenceDetailVO>> getFencesByDriverId(@PathVariable Long driverId) {
        List<FenceDetailVO> fences = fenceService.getFencesByDriverId(driverId);
        return Result.success(fences);
    }

}
