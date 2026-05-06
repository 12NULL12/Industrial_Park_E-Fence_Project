package com.fence.service;

import com.fence.entity.DeviceAlarmData;
import com.fence.entity.Vehicle;
import com.fence.mapper.VehicleMapper;
import com.fence.websocket.WebSocketWithOffline;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import com.fence.entity.GpsData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSubscribeService {
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final HeartbeatService heartbeatService;
    private final AlarmService alarmService;
    private final FenceService fenceService;
    private final VehicleMapper vehicleMapper;
    private final DispatchService dispatchService;
    private final ParkingService parkingService;


    private static final String DISPATCHER_USER_ID = "dispatcher_1";
    private static final List<String> SUBSCRIBE_TOPICS = Arrays.asList(
            "vehicle/#",
            "license/plate"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void initSubscribe() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT客户端未连接，跳过订阅初始化");
            return;
        }

        try {
            for (String topic : SUBSCRIBE_TOPICS) {
                subscribe(topic);
            }
            log.info("MQTT订阅初始化完成，共订阅 {} 个Topic", SUBSCRIBE_TOPICS.size());
        } catch (Exception e) {
            log.error("MQTT订阅初始化失败", e);
        }
    }

    private void subscribe(String topic) {
        try {
            mqttClient.subscribe(topic, 1, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    handleMessage(topic, payload);
                }
            });
            log.info("订阅MQTT Topic成功: {}", topic);
        } catch (MqttException e) {
            log.error("订阅MQTT Topic失败: {}", topic, e);
        }
    }

    private void handleMessage(String topic, String payload) {
        log.info("收到MQTT消息: topic={}, payload={}", topic, payload);

        try {
            if ("license/plate".equals(topic)) {
                handleLicensePlate(payload);
            } else {
                String vehicleId = extractvehicleId(topic);
                if (vehicleId == null || vehicleId.isEmpty()) {
                    log.warn("无法从Topic中提取设备ID: {}", topic);
                    return;
                }
                if (topic.endsWith("/heartbeat")) {
                    handleHeartbeat(vehicleId, payload);
                } else if (topic.endsWith("/gps")) {
                    handleLocation(vehicleId, payload);
                } else if (topic.endsWith("/alarm")) {
                    handleDeviceAlarm(vehicleId, payload);
                } else if (topic.endsWith("/alarm/ack")) {
                    handleAlarmAcknowledge(vehicleId, payload);
                }
            }
        } catch (Exception e) {
            log.error("处理MQTT消息失败: topic={}", topic, e);
        }
    }
    
    private void handleLicensePlate(String payload) {
        log.info("处理车牌扫描消息: payload={}", payload);
        
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            
            String plate = (String) data.get("plate");
            
            if (plate == null || plate.trim().isEmpty()) {
                log.warn("车牌扫描数据中缺少plateNumber字段");
                return;
            }
            
            parkingService.handleLicensePlateScan(plate);
            
        } catch (Exception e) {
            log.error("处理车牌扫描消息失败: payload={}", payload, e);
        }
    }
    private void handleAlarmAcknowledge(String vehicleId, String payload) {
        log.info("处理设备告警确认: vehicleId={}, payload={}", vehicleId, payload);

        try {
            Map<String, Object> ackData = objectMapper.readValue(payload, Map.class);

            String alarmType = (String) ackData.get("alarmType");
            String status = (String) ackData.get("status");
            String message = (String) ackData.get("message");
            String timestamp = (String) ackData.get("timestamp");

            if (alarmType == null) {
                log.warn("告警确认数据不完整: alarmType={}", alarmType);
                return;
            }

            Long vehicleIdLong = parseVehicleId(vehicleId);
            if (vehicleIdLong == null) {
                log.error("无法解析车辆ID: {}", vehicleId);
                return;
            }

            dispatchService.handleInstructionAck(vehicleIdLong, alarmType, status, message);

        } catch (Exception e) {
            log.error("处理告警确认失败: vehicleId={}, payload={}", vehicleId, payload, e);
        }
    }


    private void handleHeartbeat(String vehicleId, String payload) {
        log.debug("处理设备心跳: vehicleId={}, payload={}", vehicleId, payload);
        try {
            heartbeatService.heartbeat(vehicleId, payload);
        } catch (Exception e) {
            log.error("处理心跳失败: vehicleId={}", vehicleId, e);
        }
    }

    private void handleLocation(String vehicleId, String payload) {
        log.debug("处理车辆位置: vehicleId={}, location={}", vehicleId, payload);

        try {
            GpsData gpsData = objectMapper.readValue(payload, GpsData.class);
            if (gpsData.getVehicleId() == null) {
                try {
                    gpsData.setVehicleId(Long.parseLong(vehicleId));
                } catch (NumberFormatException e) {
                    log.error("无法解析vehicleId: {}", vehicleId);
                    return;
                }
            }

            if (gpsData.getLatitude() == null || gpsData.getLongitude() == null) {
                log.warn("GPS数据不完整: vehicleId={}, lat={}, lng={}",
                        vehicleId, gpsData.getLatitude(), gpsData.getLongitude());
                return;
            }

            if (gpsData.getLatitude() < -90 || gpsData.getLatitude() > 90 ||
                    gpsData.getLongitude() < -180 || gpsData.getLongitude() > 180) {
                log.warn("GPS坐标超出范围: vehicleId={}, lat={}, lng={}",
                        vehicleId, gpsData.getLatitude(), gpsData.getLongitude());
                return;
            }

            log.info("解析GPS数据成功: vehicleId={}, lat={}, lng={}, speed={}, time={}",
                    vehicleId, gpsData.getLatitude(), gpsData.getLongitude(),
                    gpsData.getSpeed(), gpsData.getTimestamp());

            String locationKey = "vehicle:position:" + vehicleId;
            redisTemplate.opsForValue().set(locationKey, payload, 60, TimeUnit.SECONDS);

            String onlineKey = "vehicle:online:" + vehicleId;
            redisTemplate.opsForValue().set(onlineKey, String.valueOf(System.currentTimeMillis()), 120, TimeUnit.SECONDS);

            log.info("车辆位置已存入Redis: vehicleId={}, key={}", vehicleId, locationKey);

            pushGpsToFrontend(gpsData);
            checkFenceAlarmAndNotify(gpsData);


        } catch (Exception e) {
            log.error("处理位置消息失败: vehicleId={}, payload={}", vehicleId, payload, e);
        }
    }
    private void pushGpsToFrontend(GpsData gpsData) {
        try {
            Map<String, Object> gpsMessage = new HashMap<>();
            gpsMessage.put("type", "GPS_UPDATE");
            gpsMessage.put("vehicleId", gpsData.getVehicleId());
            gpsMessage.put("latitude", gpsData.getLatitude());
            gpsMessage.put("longitude", gpsData.getLongitude());
            gpsMessage.put("speed", gpsData.getSpeed());
            gpsMessage.put("direction", gpsData.getDirection());
            gpsMessage.put("timestamp", gpsData.getTimestamp());
            gpsMessage.put("serverTime", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(gpsMessage);

            Set<String> targetUserIds = new HashSet<>();

            targetUserIds.add(DISPATCHER_USER_ID);
            if (vehicleMapper != null) {
                try {
                    Vehicle vehicle = vehicleMapper.selectById(gpsData.getVehicleId());
                    if (vehicle != null && vehicle.getDriverId() != null) {
                        String driverUserId = "driver_" + vehicle.getDriverId();
                        targetUserIds.add(driverUserId);
                        log.debug("添加司机到推送目标: userId={}, vehicleId={}",
                                driverUserId, gpsData.getVehicleId());
                    } else {
                        log.warn("车辆未分配司机或车辆不存在: vehicleId={}", gpsData.getVehicleId());
                    }
                } catch (Exception e) {
                    log.error("查询车辆信息失败: vehicleId={}", gpsData.getVehicleId(), e);
                }
            }
            log.debug("已向前端推送GPS数据: vehicleId={}, lat={}, lng={}",
                    gpsData.getVehicleId(), gpsData.getLatitude(), gpsData.getLongitude());
            for (String userId : targetUserIds) {
                try {
                    WebSocketWithOffline.sendToUser(userId, message);
                } catch (Exception e) {
                    log.error("推送告警给单个用户失败: userId={}, alarmId={}", userId,  e);
                }
            }

        } catch (Exception e) {
            log.error("推送GPS数据到前端失败: vehicleId={}", gpsData.getVehicleId(), e);
        }
    }
    private void checkFenceAlarmAndNotify(GpsData gpsData) {
        try {
            Long vehicleId =  gpsData.getVehicleId();
            if (vehicleId == null) {
                log.warn("GPS数据中缺少vehicleId字段");
                return;
            }

            Map<String, Object> alarmResult = fenceService.checkFenceAlarm(
                    vehicleId,
                    gpsData.getLongitude(),
                    gpsData.getLatitude()
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alarms = (List<Map<String, Object>>) alarmResult.get("alarms");

            if (alarms == null || alarms.isEmpty()) {
                return;
            }

            for (Map<String, Object> alarmInfo : alarms) {
                Boolean shouldAlarm = (Boolean) alarmInfo.get("shouldAlarm");
                if (Boolean.TRUE.equals(shouldAlarm)) {
                    processFenceAlarm(vehicleId, gpsData, alarmInfo);
                }
            }

        } catch (Exception e) {
            log.error("检查围栏告警失败: vehicleId={}", gpsData.getVehicleId(), e);
        }
    }

    private void processFenceAlarm(Long vehicleId, GpsData gpsData, Map<String, Object> alarmInfo) {
        try {
            Long fenceId = (Long) alarmInfo.get("fenceId");
            String fenceName = (String) alarmInfo.get("fenceName");

            Vehicle vehicle = vehicleMapper.selectById(vehicleId);
            if (vehicle == null) {
                log.warn("车辆不存在: vehicleId={}", vehicleId);
                return;
            }

            String content = String.format("车辆%s(%d)进入禁区[%s]",
                    vehicle.getPlateNumber(),
                    vehicleId,
                    fenceName);

            Long alarmId = alarmService.generateAlarm(
                    "OUT_FENCE",
                    vehicleId,
                    gpsData.getLongitude(),
                    gpsData.getLatitude(),
                    content
            );

            log.info("围栏告警处理完成: alarmId={}, vehicleId={}, fenceId={}",
                    alarmId, vehicleId, fenceId);

        } catch (Exception e) {
            log.error("处理围栏告警失败: vehicleId={}", vehicleId, e);
        }
    }



    private void handleDeviceAlarm(String vehicleId, String payload) {
        log.info("处理设备告警: vehicleId={}, payload={}", vehicleId, payload);

        try {
            DeviceAlarmData alarmData = objectMapper.readValue(payload, DeviceAlarmData.class);

            if (alarmData.getVehicleId() == null || alarmData.getAlarmType() == null) {
                log.warn("告警数据不完整: vehicleId={}, alarmType={}",
                        alarmData.getVehicleId(), alarmData.getAlarmType());
                return;
            }

            Long vehicleIdLong = parseVehicleId(alarmData.getVehicleId());
            if (vehicleIdLong == null) {
                log.error("无法解析车辆ID: {}", alarmData.getVehicleId());
                return;
            }

            String alarmType = convertAlarmType(alarmData.getAlarmType());
            String content = alarmData.getMessage();
            Double longitude = alarmData.getLongitude();
            Double latitude = alarmData.getLatitude();

            if (alarmService != null) {
                Long alarmId = alarmService.generateAlarm(
                        alarmType,
                        vehicleIdLong,
                        longitude,
                        latitude,
                        content
                );

                if (alarmId != null) {
                    log.info("设备告警处理成功: alarmId={}, type={}, vehicleId={}",
                            alarmId, alarmType, vehicleIdLong);
                }
            } else {
                log.warn("AlarmService未注入，无法生成告警");
            }

        } catch (Exception e) {
            log.error("处理告警消息失败: vehicleId={}, payload={}", vehicleId, payload, e);
        }
    }
    private Long parseVehicleId(String vehicleId) {
        if (vehicleId == null || vehicleId.isEmpty()) {
            return null;
        }

        // 如果是纯数字，直接转换
        if (vehicleId.matches("\\d+")) {
            return Long.parseLong(vehicleId);
        }

        // 如果是 "vehicle_001" 格式，提取数字部分
        if (vehicleId.startsWith("vehicle_")) {
            String numberPart = vehicleId.substring(8);
            if (numberPart.matches("\\d+")) {
                return Long.parseLong(numberPart);
            }
        }

        log.warn("无法解析车辆ID: {}", vehicleId);
        return null;
    }

    private String extractvehicleId(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }
    private String convertAlarmType(String deviceAlarmType) {
        if (deviceAlarmType == null) {
            return "UNKNOWN";
        }

        switch (deviceAlarmType.toLowerCase()) {
            case "overspeed":
            case "speed_overdue":
                return com.fence.entity.AlarmType.SPEED_OVERDUE;
            case "smoke":
            case "fire":
                return com.fence.entity.AlarmType.DEVICE_FAULT;
            case "collision":
            case "crash":
                return com.fence.entity.AlarmType.DEVICE_FAULT;
            case "power_off":
            case "low_battery":
                return com.fence.entity.AlarmType.DEVICE_FAULT;
            default:
                log.warn("未知的告警类型: {}, 使用默认类型", deviceAlarmType);
                return "DEVICE_FAULT";
        }
    }
}
