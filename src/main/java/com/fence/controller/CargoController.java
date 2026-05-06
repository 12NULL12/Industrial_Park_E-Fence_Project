package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.cargo.CargoBindRequest;
import com.fence.dto.cargo.CargoCreateRequest;
import com.fence.dto.cargo.CargoDetailVO;
import com.fence.service.CargoBindService;
import com.fence.service.CargoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cargo")
public class CargoController {

    @Autowired
    private CargoService cargoService;

    @Autowired
    private CargoBindService cargoBindService;

    /**
     * 14.1 获取货物列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getList() {
        List<CargoDetailVO> list = cargoService.getCargoList();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);

        return Result.success(data);
    }

    /**
     * 14.2 获取货物详情
     */
    @GetMapping("/{id}")
    public Result<CargoDetailVO> getDetail(@PathVariable Long id) {
        CargoDetailVO detail = cargoService.getCargoDetail(id);
        return Result.success(detail);
    }

    /**
     * 14.3 新增货物
     */
    @PostMapping
    public Result<Long> create(@RequestBody CargoCreateRequest request) {
        Long cargoId = cargoService.createCargo(request);
        return Result.success(cargoId);
    }

    /**
     * 14.4 修改货物
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CargoCreateRequest request) {
        cargoService.updateCargo(id, request);
        return Result.success();
    }

    /**
     * 14.5 绑定货物到任务
     */
    @PostMapping("/bind")
    public Result<Void> bind(@RequestBody CargoBindRequest request) {
        cargoBindService.bindCargoByCode(request);
        return Result.success();
    }
    /**
     * 14.6 标记货物已送达
     */
    @PostMapping("/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id) {
        cargoService.markAsDelivered(id);
        return Result.success();
    }
}
