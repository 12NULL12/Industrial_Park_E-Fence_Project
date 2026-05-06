package com.fence.controller;

import com.fence.common.Result;
import com.fence.service.ParkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @PostMapping("/confirm/{recordId}")
    public Result<Void> confirmExternalVehicle(
            @PathVariable Long recordId,
            @RequestHeader("X-User-Id") Long confirmBy,
            @RequestHeader("X-User-Name") String confirmName) {

        log.info("收到外来车辆确认请求: recordId={}, confirmBy={}", recordId, confirmBy);

        parkingService.confirmExternalVehicle(recordId, confirmBy, confirmName);

        return Result.success();
    }

    @GetMapping("/record/{id}")
    public Result<?> getParkingRecord(@PathVariable Long id) {
        return Result.success(parkingService.getParkingRecord(id));
    }

    @GetMapping("/records")
    public Result<?> getParkingRecords(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "100") int limit) {

        return Result.success(parkingService.getParkingRecords(plateNumber, action, limit));
    }
}