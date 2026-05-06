package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.dto.alarm.AlarmDetailVO;
import com.fence.dto.alarm.AlarmHandleRequest;
import com.fence.entity.Alarm;
import com.fence.entity.AlarmLevel;
import com.fence.entity.AlarmType;
import com.fence.entity.Vehicle;
import com.fence.mapper.AlarmMapper;
import com.fence.mapper.VehicleMapper;
import com.fence.websocket.WebSocketWithOffline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlarmService {

    @Autowired
    private AlarmMapper alarmMapper;

    @Autowired(required = false)
    private VehicleMapper vehicleMapper;

    @Autowired(required = false)
    private com.fence.mapper.DriverMapper driverMapper;
    @Autowired
    private MqttPublishService mqttPublishService;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    private static final String ALARM_DEDUP_KEY_PREFIX = "alarm:dedup:";
    private static final long DEDUP_WINDOW_SECONDS = 300;
    private static final String DISPATCHER_USER_ID = "dispatcher_1";

    /**
     * 生成告警（核心方法 - 由各种算法检测后调用）
     *
     * @param alarmType 告警类型
     * @param vehicleId 车辆ID
     * @param longitude 经度
     * @param latitude 纬度
     * @param content 告警内容
     * @return 告警ID
     */
    @Transactional
    public Long generateAlarm(String alarmType, Long vehicleId,
                              Double longitude, Double latitude,
                              String content) {
        boolean shouldDedup = !"EXTERNAL_VEHICLE".equals(alarmType);
        
        if (shouldDedup) {
            String dedupKey = ALARM_DEDUP_KEY_PREFIX + vehicleId + ":" + alarmType;

            if (redisTemplate != null) {
                Boolean exists = redisTemplate.hasKey(dedupKey);
                if (Boolean.TRUE.equals(exists)) {
                    log.debug("告警去重：车辆在时间窗口内已触发相同类型告警: vehicleId={}, type={}",
                            vehicleId, alarmType);
                    return null;
                }

                redisTemplate.opsForValue().set(dedupKey, "1", DEDUP_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        } else {
            log.debug("外来车辆告警跳过去重: content={}", content);
        }

        Alarm alarm = new Alarm();
        alarm.setAlarmNo(generateAlarmNo());
        alarm.setVehicleId(vehicleId);
        
        // 如果是外来车辆告警，从content中提取车牌号
        if ("EXTERNAL_VEHICLE".equals(alarmType) && content != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(.*?)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                alarm.setVehiclePlate(matcher.group(1));
            }
        }
        
        alarm.setAlarmType(alarmType);
        alarm.setAlarmLevel(determineAlarmLevel(alarmType));
        alarm.setAlarmContent(content);
        alarm.setLongitude(longitude);
        alarm.setLatitude(latitude);
        alarm.setStatus("PENDING");
        alarm.setAlarmTime(LocalDateTime.now());
        alarm.setCreateTime(LocalDateTime.now());

        alarmMapper.insert(alarm);

        log.info("生成告警: alarmNo={}, type={}, vehicleId={}, vehiclePlate={}",
                alarm.getAlarmNo(), alarmType, vehicleId, alarm.getVehiclePlate());

        pushAlarmToTargetUsers(alarm);

        return alarm.getId();
    }

    /**
     * 查询告警列表
     */
    public List<AlarmDetailVO> queryAlarms(int limit,
                                           String status,
                                           String alarmType,
                                           String alarmLevel,
                                           Long vehicleId,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("alarmType", alarmType);
        params.put("alarmLevel", alarmLevel);
        params.put("vehicleId", vehicleId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("limit", limit);
        params.put("offset", 0);

        List<Alarm> alarms = alarmMapper.selectList(params);
        
        // 为每条告警关联查询车辆和司机信息
        return alarms.stream()
                .map(alarm -> {
                    AlarmDetailVO vo = convertToVO(alarm);
                    
                    // 关联查询车辆信息
                    if (vehicleMapper != null && alarm.getVehicleId() != null) {
                        try {
                            Vehicle vehicle = vehicleMapper.selectById(alarm.getVehicleId());
                            if (vehicle != null) {
                                vo.setVehiclePlate(vehicle.getPlateNumber());
                                
                                // 查询司机信息
                                if (vehicle.getDriverId() != null && driverMapper != null) {
                                    com.fence.entity.Driver driver = driverMapper.selectById(vehicle.getDriverId());
                                    if (driver != null) {
                                        vo.setDriverName(driver.getName());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("查询车辆信息失败: vehicleId={}", alarm.getVehicleId(), e);
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询告警详情
     */
    public AlarmDetailVO getAlarmDetail(Long id) {
        Alarm alarm = alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BusinessException("告警不存在");
        }
        
        AlarmDetailVO vo = convertToVO(alarm);
        
        // 关联查询车辆和司机信息
        if (vehicleMapper != null && alarm.getVehicleId() != null) {
            try {
                Vehicle vehicle = vehicleMapper.selectById(alarm.getVehicleId());
                if (vehicle != null) {
                    vo.setVehiclePlate(vehicle.getPlateNumber());
                    
                    // 如果有司机ID，查询司机姓名
                    if (vehicle.getDriverId() != null && driverMapper != null) {
                        com.fence.entity.Driver driver = driverMapper.selectById(vehicle.getDriverId());
                        if (driver != null) {
                            vo.setDriverName(driver.getName());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("查询车辆信息失败: vehicleId={}", alarm.getVehicleId(), e);
            }
        }
        return vo;
    }

    /**
     * 处理告警
     */
    @Transactional
    public void handleAlarm(AlarmHandleRequest request, Long handlerId, String handlerName) {
        Alarm alarm = alarmMapper.selectById(request.getAlarmId());
        if (alarm == null) {
            throw new BusinessException("告警不存在");
        }

        if (!"PENDING".equals(alarm.getStatus())) {
            throw new BusinessException("告警已处理，无法重复操作");
        }

        validateHandleMethod(request.getHandleMethod());

        String newStatus = determineNewStatus(request.getHandleMethod());

        alarmMapper.updateStatus(
                request.getAlarmId(),
                newStatus,
                request.getHandleMethod(),
                request.getHandleRemark(),
                handlerId,
                handlerName
        );

        log.info("处理告警: alarmId={}, method={}, handler={}",
                request.getAlarmId(), request.getHandleMethod(), handlerName);

        // 如果是紧急停车或临时停车，通过MQTT发送指令到设备端
        if ("EMERGENCY_STOP".equals(request.getHandleMethod()) || 
            "TEMP_STOP".equals(request.getHandleMethod())) {
            sendStopCommandToDevice(alarm.getVehicleId(), request.getHandleMethod());
        }
    }

    /**
     * 发送停车指令到设备端
     */
    private void sendStopCommandToDevice(Long vehicleId, String commandType) {
        try {
            // 查询车辆信息获取设备ID
            Vehicle vehicle = vehicleMapper.selectById(vehicleId);
            if (vehicle == null) {
                log.warn("车辆不存在: vehicleId={}", vehicleId);
                return;
            }

            // 构建MQTT消息
            Map<String, Object> command = new HashMap<>();
            command.put("command", commandType);
            command.put("vehicleId", vehicleId);
            command.put("timestamp", LocalDateTime.now().toString());
            command.put("message", "EMERGENCY_STOP".equals(commandType) ? "紧急停车指令" : "临时停车指令");

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String message = objectMapper.writeValueAsString(command);

            // 发布到MQTT主题：vehicle/{vehicleId}/command
            String topic = "vehicle/vehicle_00" + vehicleId + "/command";
            mqttPublishService.publish(topic, message);

            log.info("停车指令已发送到设备端: vehicleId={}, command={}, topic={}",
                    vehicleId, commandType, topic);

        } catch (Exception e) {
            log.error("发送停车指令失败: vehicleId={}, command={}", vehicleId, commandType, e);
        }
    }

    /**
     * 批量处理告警
     */
    @Transactional
    public void batchHandleAlarms(List<Long> alarmIds, String handleMethod,
                                  String remark, Long handlerId, String handlerName) {
        for (Long alarmId : alarmIds) {
            AlarmHandleRequest request = new AlarmHandleRequest();
            request.setAlarmId(alarmId);
            request.setHandleMethod(handleMethod);
            request.setHandleRemark(remark);
            handleAlarm(request, handlerId, handlerName);
        }
    }
    @Transactional
    public void handleDeviceAlarmAck(Long vehicleId, String alarmType, String status, String message) {
        log.info("处理设备告警确认: vehicleId={}, alarmType={}, status={}",
                vehicleId, alarmType, status);

        Alarm latestAlarm = alarmMapper.selectLatestByVehicleAndType(vehicleId, alarmType);

        if (latestAlarm == null) {
            log.warn("未找到对应的告警记录: vehicleId={}, alarmType={}", vehicleId, alarmType);
            return;
        }

        if (!"PENDING".equals(latestAlarm.getStatus())) {
            log.warn("告警已处理，无需重复确认: alarmId={}, status={}",
                    latestAlarm.getId(), latestAlarm.getStatus());
            return;
        }

        String newStatus = "CONFIRMED";
        if ("received".equalsIgnoreCase(status)) {
            newStatus = "CONFIRMED";
        } else if ("handling".equalsIgnoreCase(status)) {
            newStatus = "PROCESSING";
        } else if ("resolved".equalsIgnoreCase(status)) {
            newStatus = "RESOLVED";
        }

        alarmMapper.updateStatus(
                latestAlarm.getId(),
                newStatus,
                "DEVICE_ACK",
                message != null ? message : "设备端确认",
                null,
                "DEVICE"
        );

        log.info("设备告警确认成功: alarmId={}, newStatus={}", latestAlarm.getId(), newStatus);

        pushAlarmStatusUpdateToFrontend(latestAlarm, newStatus, message);
    }
    private void pushAlarmStatusUpdateToFrontend(Alarm alarm, String newStatus, String message) {
        try {
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("type", "ALARM_STATUS_UPDATE");
            statusMessage.put("alarmId", alarm.getId());
            statusMessage.put("alarmNo", alarm.getAlarmNo());
            statusMessage.put("vehicleId", alarm.getVehicleId());
            statusMessage.put("alarmType", alarm.getAlarmType());
            statusMessage.put("alarmLevel", alarm.getAlarmLevel());
            statusMessage.put("status", newStatus);
            statusMessage.put("message", message);
            statusMessage.put("updateTime", LocalDateTime.now().toString());
            statusMessage.put("serverTime", System.currentTimeMillis());

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(statusMessage);

            Set<String> targetUserIds = new HashSet<>();

            targetUserIds.add(DISPATCHER_USER_ID);
            log.debug("添加调度员到推送目标: userId=dispatcher_1");

            if (vehicleMapper != null) {
                try {
                    Vehicle vehicle = vehicleMapper.selectById(alarm.getVehicleId());
                    if (vehicle != null && vehicle.getDriverId() != null) {
                        String driverUserId = "driver_" + vehicle.getDriverId();
                        targetUserIds.add(driverUserId);
                        log.debug("添加司机到推送目标: userId={}, vehicleId={}",
                                driverUserId, alarm.getVehicleId());
                    }
                } catch (Exception e) {
                    log.error("查询车辆信息失败: vehicleId={}", alarm.getVehicleId(), e);
                }
            }

            for (String userId : targetUserIds) {
                try {
                    WebSocketWithOffline.sendToUser(userId, jsonMessage);
                    log.debug("推送告警状态给 {}: status={}, alarmId={}",
                            userId, newStatus, alarm.getId());
                } catch (Exception e) {
                    log.error("推送告警状态给单个用户失败: userId={}, alarmId={}",
                            userId, alarm.getId(), e);
                }
            }

            log.info("告警状态已推送到前端: alarmId={}, status={}, targets={}",
                    alarm.getId(), newStatus, targetUserIds.size());

        } catch (Exception e) {
            log.error("推送告警状态到前端失败: alarmId={}", alarm.getId(), e);
        }
    }

    /**
     * 告警统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", alarmMapper.countByStatus(null));
        result.put("pending", alarmMapper.countByStatus("PENDING"));
        result.put("confirmed", alarmMapper.countByStatus("CONFIRMED"));
        result.put("processing", alarmMapper.countByStatus("PROCESSING"));
        result.put("resolved", alarmMapper.countByStatus("RESOLVED"));
        result.put("falseAlarm", alarmMapper.countByStatus("FALSE_ALARM"));
        result.put("byType", alarmMapper.statisticsByType());
        result.put("byLevel", alarmMapper.statisticsByLevel());
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private String generateAlarmNo() {
        return "ALM" + System.currentTimeMillis();
    }

    private String determineAlarmLevel(String alarmType) {
        switch (alarmType) {
            case AlarmType.OUT_FENCE:
                return AlarmLevel.HIGH;
            case AlarmType.SPEED_OVERDUE:
            case AlarmType.TASK_TIMEOUT:
                return AlarmLevel.MEDIUM;
            case AlarmType.DEVICE_FAULT:
            case AlarmType.OFFLINE:
                return AlarmLevel.LOW;
            default:
                return AlarmLevel.MEDIUM;
        }
    }

    private void validateHandleMethod(String handleMethod) {
        List<String> validMethods = Arrays.asList(
                "ADJUST_ROUTE", "TEMP_STOP", "EMERGENCY_STOP", "IGNORE"
        );
        if (!validMethods.contains(handleMethod)) {
            throw new BusinessException("无效的处理方式");
        }
    }

    private String determineNewStatus(String handleMethod) {
        switch (handleMethod) {
            case "IGNORE":
                return "FALSE_ALARM";
            case "ADJUST_ROUTE":
            case "TEMP_STOP":
            case "EMERGENCY_STOP":
                return "PROCESSING";
            default:
                return "CONFIRMED";
        }
    }

    /**
     * 定向推送告警到指定用户（调度员 + 司机）
     */
    private void pushAlarmToTargetUsers(Alarm alarm) {
        try {
            Map<String, Object> alarmMessage = new HashMap<>();
            alarmMessage.put("type", "ALARM_TRIGGERED");
            alarmMessage.put("alarmId", alarm.getId());
            alarmMessage.put("alarmNo", alarm.getAlarmNo());
            alarmMessage.put("vehicleId", alarm.getVehicleId());
            alarmMessage.put("alarmType", alarm.getAlarmType());
            alarmMessage.put("alarmLevel", alarm.getAlarmLevel());
            alarmMessage.put("content", alarm.getAlarmContent());
            alarmMessage.put("longitude", alarm.getLongitude());
            alarmMessage.put("latitude", alarm.getLatitude());
            alarmMessage.put("alarmTime", alarm.getAlarmTime().toString());
            alarmMessage.put("serverTime", System.currentTimeMillis());

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String message = objectMapper.writeValueAsString(alarmMessage);

            Set<String> targetUserIds = new HashSet<>();

            targetUserIds.add(DISPATCHER_USER_ID);
            log.debug("添加调度员到推送目标: userId={}", DISPATCHER_USER_ID);

            if (vehicleMapper != null) {
                try {
                    Vehicle vehicle = vehicleMapper.selectById(alarm.getVehicleId());
                    if (vehicle != null && vehicle.getDriverId() != null) {
                        String driverUserId = "driver_" + vehicle.getDriverId();
                        targetUserIds.add(driverUserId);
                        log.debug("添加司机到推送目标: userId={}, vehicleId={}",
                                driverUserId, alarm.getVehicleId());
                    } else {
                        log.warn("车辆未分配司机或车辆不存在: vehicleId={}", alarm.getVehicleId());
                    }
                } catch (Exception e) {
                    log.error("查询车辆信息失败: vehicleId={}", alarm.getVehicleId(), e);
                }
            }

            int successCount = 0;
            int failCount = 0;
            for (String userId : targetUserIds) {
                try {
                    WebSocketWithOffline.sendToUser(userId, message);
                    successCount++;
                    log.debug("告警已推送给用户: userId={}, alarmId={}", userId, alarm.getId());
                } catch (Exception e) {
                    failCount++;
                    log.error("推送告警给单个用户失败: userId={}, alarmId={}", userId, alarm.getId(), e);
                }
            }

            log.info("告警定向推送完成: 成功={}, 失败={}, 总数={}, alarmId={}, type={}",
                    successCount, failCount, targetUserIds.size(), alarm.getId(), alarm.getAlarmType());

        } catch (Exception e) {
            log.error("推送告警到前端失败: alarmId={}, alarmNo={}, vehicleId={}",
                    alarm.getId(), alarm.getAlarmNo(), alarm.getVehicleId(), e);
        }
    }

    private AlarmDetailVO convertToVO(Alarm alarm) {
        AlarmDetailVO vo = new AlarmDetailVO();
        BeanUtils.copyProperties(alarm, vo);
        return vo;
    }
}
