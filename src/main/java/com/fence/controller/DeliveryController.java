package com.fence.controller;

import com.fence.common.Result;
import com.fence.dto.cargo.CargoBindRequest;
import com.fence.dto.task.TaskArriveRequest;
import com.fence.service.CargoBindService;
import com.fence.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private CargoBindService cargoBindService;

    /**
     * 4.4 确认送达
     */
    @PostMapping("/confirm-arrive")
    public Result<String> confirmArrive(@RequestBody TaskArriveRequest request) {
        String message = deliveryService.confirmArrive(request);
        return Result.success(message);
    }
    @PostMapping("/bind-cargo")
    public Result<Void> bindCargo(@RequestBody CargoBindRequest request) {
        cargoBindService.bindCargoByCode(request);
        return Result.success();
    }
}

