package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.entity.ParkingRecord;
import com.fence.entity.Vehicle;
import com.fence.mapper.ParkingRecordMapper;
import com.fence.mapper.VehicleMapper;
import com.fence.websocket.WebSocketWithOffline;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRecordMapper parkingRecordMapper;
    private final VehicleMapper vehicleMapper;
    private final AlarmService alarmService;
    private final ObjectMapper objectMapper;

    private static final String DISPATCHER_USER_ID = "dispatcher_1";

    public static final String VEHICLE_TYPE_PARK = "PARK_VEHICLE";
    public static final String VEHICLE_TYPE_EXTERNAL = "EXTERNAL_VEHICLE";
    public static final String ACTION_ENTER = "ENTER";
    public static final String ACTION_EXIT = "EXIT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    @Transactional
    public void handleLicensePlateScan(String plateNumber) {
        log.info("处理车牌扫描: plateNumber={}", plateNumber);

        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            log.warn("车牌号为空");
            return;
        }

        plateNumber = plateNumber.trim();

        ParkingRecord existingRecord = parkingRecordMapper.selectByPlateNumber(plateNumber);

        if (existingRecord != null) {
            handleExit(plateNumber, existingRecord);
        } else {
            handleEnter(plateNumber);
        }
    }

    private void handleEnter(String plateNumber) {
        log.info("车辆入园: plateNumber={}", plateNumber);

        Vehicle vehicle = vehicleMapper.selectByPlateNumber(plateNumber);

        String vehicleType;
        Long alarmId = null;

        if (vehicle != null) {
            vehicleType = VEHICLE_TYPE_PARK;
            log.info("园区车辆入园: plateNumber={}, vehicleId={}", plateNumber, vehicle.getId());
        } else {
            vehicleType = VEHICLE_TYPE_EXTERNAL;
            log.warn("外来车辆入园: plateNumber={}", plateNumber);
        }

        ParkingRecord record = new ParkingRecord();
        record.setPlateNumber(plateNumber);
        record.setVehicleType(vehicleType);
        record.setAction(ACTION_ENTER);
        record.setStatus(vehicleType.equals(VEHICLE_TYPE_EXTERNAL) ? STATUS_PENDING : STATUS_CONFIRMED);
        record.setEnterTime(LocalDateTime.now());
        record.setRemark(vehicleType.equals(VEHICLE_TYPE_EXTERNAL) ? "外来车辆，需调度员确认" : "园区车辆");

        parkingRecordMapper.insert(record);
            
        log.info("入园记录已保存: id={}, plateNumber={}, vehicleType={}", 
                record.getId(), plateNumber, record.getVehicleType());
            
        if (VEHICLE_TYPE_EXTERNAL.equals(record.getVehicleType())) {
            log.warn("外来车辆入园: plateNumber={}", plateNumber);
                
            alarmId = generateExternalVehicleAlarmAfterInsert(plateNumber, record.getId());
                
            if (alarmId != null) {
                record.setAlarmId(alarmId);
                parkingRecordMapper.update(record);
                log.info("外来车辆告警已生成: alarmId={}, plateNumber={}, recordId={}", 
                        alarmId, plateNumber, record.getId());
            }
                
        }
        
    }

    private void handleExit(String plateNumber, ParkingRecord enterRecord) {
        log.info("车辆出园: plateNumber={}, enterRecordId={}", plateNumber, enterRecord.getId());

        parkingRecordMapper.deleteById(enterRecord.getId());

        log.info("入园记录已删除: id={}, plateNumber={}", enterRecord.getId(), plateNumber);
    }

    private Long generateExternalVehicleAlarm(String plateNumber) {
        try {
            String content = String.format("外来车辆[%s]入园，请调度员确认", plateNumber);

            Long alarmId = alarmService.generateAlarm(
                    "EXTERNAL_VEHICLE",
                    null,
                    null,
                    null,
                    content
            );

            log.info("外来车辆告警已生成: alarmId={}, plateNumber={}", alarmId, plateNumber);

            return alarmId;
        } catch (Exception e) {
            log.error("生成外来车辆告警失败: plateNumber={}", plateNumber, e);
            return null;
        }
    }

    private Long generateExternalVehicleAlarmAfterInsert(String plateNumber, Long recordId) {
        try {
            String content = String.format("外来车辆[%s]入园，请调度员确认", plateNumber);

            Long alarmId = alarmService.generateAlarm(
                    "EXTERNAL_VEHICLE",
                    null,
                    null,
                    null,
                    content
            );

            log.info("外来车辆告警已生成: alarmId={}, plateNumber={}, recordId={}", 
                    alarmId, plateNumber, recordId);

            return alarmId;
        } catch (Exception e) {
            log.error("生成外来车辆告警失败: plateNumber={}, recordId={}", plateNumber, recordId, e);
            return null;
        }
    }

    @Transactional
    public void confirmExternalVehicle(Long recordId, Long confirmBy, String confirmName) {
        log.info("确认外来车辆: recordId={}, confirmBy={}", recordId, confirmBy);
        
        ParkingRecord record = parkingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("入园记录不存在");
        }
        
        if (!VEHICLE_TYPE_EXTERNAL.equals(record.getVehicleType())) {
            throw new BusinessException("该记录不是外来车辆");
        }
        
        record.setStatus(STATUS_CONFIRMED);
        record.setConfirmBy(confirmBy);
        record.setConfirmName(confirmName);
        record.setConfirmTime(LocalDateTime.now());
        record.setRemark("调度员已确认");
        
        parkingRecordMapper.update(record);
        
        if (record.getAlarmId() != null) {
            try {
                com.fence.dto.alarm.AlarmHandleRequest alarmRequest = new com.fence.dto.alarm.AlarmHandleRequest();
                alarmRequest.setAlarmId(record.getAlarmId());
                alarmRequest.setHandleMethod("IGNORE");
                alarmRequest.setHandleRemark("外来车辆已确认");
                
                alarmService.handleAlarm(alarmRequest, confirmBy, confirmName);
            } catch (Exception e) {
                log.error("关闭告警失败: alarmId={}", record.getAlarmId(), e);
            }
        }
        
        log.info("外来车辆已确认: recordId={}", recordId);
    }

    public ParkingRecord getParkingRecord(Long id) {
        return parkingRecordMapper.selectById(id);
    }

    public java.util.List<ParkingRecord> getParkingRecords(String plateNumber, String action, int limit) {
        return parkingRecordMapper.selectList(plateNumber, action, limit);
    }
}