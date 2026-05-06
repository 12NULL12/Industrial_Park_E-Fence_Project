package com.fence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fence.entity.HeartbeatData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private static final String HEARTBEAT_KEY_PREFIX = "heartbeat:";
    private static final long HEARTBEAT_TIMEOUT_SECONDS = 60;

    @Autowired(required = false)
    private AlarmService alarmService;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 设备发送心跳
     */
    public void heartbeat(String vehicleId, String payload) {
        try {
            log.debug("设备 {} 收到心跳", vehicleId);

            redisTemplate.opsForValue().set(HEARTBEAT_KEY_PREFIX + vehicleId, String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(HEARTBEAT_KEY_PREFIX + vehicleId, HEARTBEAT_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("解析心跳数据失败: vehicleId={}, payload={}", vehicleId, payload, e);
        }
    }

    /**
     * 检查设备是否在线
     */
    public boolean isOnline(String vehicleId) {
        String key = HEARTBEAT_KEY_PREFIX + vehicleId;
        String lastHeartbeat = redisTemplate.opsForValue().get(key);

        if (lastHeartbeat == null) {
            return false;
        }

        long lastTime = Long.parseLong(lastHeartbeat);
        long now = System.currentTimeMillis();

        return (now - lastTime) < HEARTBEAT_TIMEOUT_SECONDS * 1000;
    }

    /**
     * 获取所有在线设备
     */
    public Set<String> getOnlineVehicles() {
        Set<String> keys = redisTemplate.keys(HEARTBEAT_KEY_PREFIX + "*");
        if (keys == null) {
            return new HashSet<>();
        }

        Set<String> onlineVehicles = new HashSet<>();
        long now = System.currentTimeMillis();

        for (String key : keys) {
            String lastHeartbeat = redisTemplate.opsForValue().get(key);
            if (lastHeartbeat != null) {
                long lastTime = Long.parseLong(lastHeartbeat);
                if ((now - lastTime) < HEARTBEAT_TIMEOUT_SECONDS * 1000) {
                    String vehicleId = key.replace(HEARTBEAT_KEY_PREFIX, "");
                    onlineVehicles.add(vehicleId);
                }
            }
        }

        return onlineVehicles;
    }

    /**
     * 定时检查任务 - 每30秒检查一次超时未心跳的车辆
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledCheck() {
        checkTimeoutVehicles();
    }

    /**
     * 检查超时未发送心跳的车辆并生成告警
     */
    private void checkTimeoutVehicles() {
        Set<String> keys = redisTemplate.keys(HEARTBEAT_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            log.debug("暂无心跳记录");
            return;
        }

        long now = System.currentTimeMillis();
        Set<String> timeoutVehicles = new HashSet<>();

        for (String key : keys) {
            String lastHeartbeatStr = redisTemplate.opsForValue().get(key);
            if (lastHeartbeatStr == null) {
                continue;
            }

            try {
                long lastHeartbeatTime = Long.parseLong(lastHeartbeatStr);
                long elapsedSeconds = (now - lastHeartbeatTime) / 1000;

                if (elapsedSeconds > HEARTBEAT_TIMEOUT_SECONDS) {
                    String vehicleId = key.replace(HEARTBEAT_KEY_PREFIX, "");
                    timeoutVehicles.add(vehicleId);
                    log.warn("车辆心跳超时: vehicleId={}, 已超时{}秒", vehicleId, elapsedSeconds);
                }
            } catch (NumberFormatException e) {
                log.error("解析心跳时间戳失败: key={}, value={}", key, lastHeartbeatStr);
            }
        }

        if (!timeoutVehicles.isEmpty()) {
            log.warn("发现 {} 个车辆心跳超时，准备生成告警", timeoutVehicles.size());

            for (String vehicleId : timeoutVehicles) {
                generateHeartbeatTimeoutAlarm(vehicleId);
            }
        } else {
            log.debug("所有车辆心跳正常");
        }
    }

    /**
     * 生成心跳超时告警
     */
    private void generateHeartbeatTimeoutAlarm(String vehicleId) {
        if (alarmService == null) {
            log.warn("AlarmService未注入，无法生成告警");
            return;
        }

        try {
            Long vehicleIdLong = parseVehicleId(vehicleId);
            if (vehicleIdLong == null) {
                log.error("无法解析车辆ID: vehicleId={}", vehicleId);
                return;
            }

            String alarmContent = String.format("车辆%s超过%d秒未发送心跳，可能离线",
                    vehicleId, HEARTBEAT_TIMEOUT_SECONDS);

            alarmService.generateAlarm(
                    com.fence.entity.AlarmType.OFFLINE,
                    vehicleIdLong,
                    null,
                    null,
                    alarmContent
            );

            log.info("已生成心跳超时告警: vehicleId={}", vehicleId);
        } catch (Exception e) {
            log.error("生成心跳超时告警失败: vehicleId={}", vehicleId, e);
        }
    }
    /**
     * 解析车辆ID（支持 "vehicle_001" 或 "1" 格式）
     */
    private Long parseVehicleId(String vehicleId) {
        if (vehicleId == null || vehicleId.isEmpty()) {
            return null;
        }
            if (vehicleId.matches("\\d+")) {

            return Long.parseLong(vehicleId);
        }

        if (vehicleId.startsWith("vehicle_")) {
            String numberPart = vehicleId.substring(8);
            if (numberPart.matches("\\d+")) {
                return Long.parseLong(numberPart);
            }
        }

        log.warn("无法解析车辆ID: {}", vehicleId);
        return null;
    }

    /**
     * 更新车辆状态（仅用于内部状态管理，不触发告警）
     */
    private void updateVehicleStatus(String vehicleId, boolean online) {
        String key = "vehicle:status:" + vehicleId;
        String status = online ? "online" : "offline";

        redisTemplate.opsForValue().set(key, status);
        log.debug("车辆状态已更新: vehicleId={}, status={}", vehicleId, status);
    }
}
