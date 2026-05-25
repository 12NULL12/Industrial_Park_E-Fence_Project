package com.fence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 指令下发确认服务（基于Redis Stream）
 * 
 * 优化场景：可靠性场景 - 确保指令送达并执行
 * 
 * 问题：
 * - 当前无法确认设备是否收到指令
 * - 无法确认设备是否执行成功
 * - 失败后没有自动重试机制
 * 
 * 解决方案：
 * - 指令下发后进入Pending状态
 * - 等待设备ACK确认
 * - 超时未确认自动重试（最多3次）
 * - 记录完整的指令生命周期
 */
@Slf4j
@Service
public class CommandAckStreamService {

    private static final String COMMAND_STREAM_KEY = "command:ack:stream";
    private static final String COMMAND_GROUP_NAME = "command-ack-group";
    private static final String COMMAND_CONSUMER_NAME = "command-ack-consumer-1";
    
    private static final String PENDING_TRACKING_KEY = "command:pending:";
    private static final long COMMAND_TIMEOUT_SECONDS = 30;  // 指令超时时间30秒
    private static final int MAX_RETRY_COUNT = 3;  // 最多重试3次

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MqttPublishService mqttPublishService;

    /**
     * 下发指令并跟踪确认
     * 
     * @param vehicleId 车辆ID
     * @param commandType 指令类型
     * @param commandData 指令数据
     * @return 指令ID
     */
    public String sendCommandWithAck(Long vehicleId, String commandType, Map<String, Object> commandData) {
        try {
            String commandId = generateCommandId();
            
            Map<String, String> messageBody = new HashMap<>();
            messageBody.put("commandId", commandId);
            messageBody.put("vehicleId", String.valueOf(vehicleId));
            messageBody.put("commandType", commandType);
            messageBody.put("commandData", objectMapper.writeValueAsString(commandData));
            messageBody.put("status", "SENT");
            messageBody.put("retryCount", "0");
            messageBody.put("sendTime", String.valueOf(System.currentTimeMillis()));
            messageBody.put("timeout", String.valueOf(COMMAND_TIMEOUT_SECONDS));

            // XADD 添加消息
            redisTemplate.opsForStream().add(COMMAND_STREAM_KEY, messageBody);

            // 记录Pending状态
            Map<String, String> trackingInfo = new HashMap<>();
            trackingInfo.put("commandId", commandId);
            trackingInfo.put("status", "SENT");
            trackingInfo.put("retryCount", "0");
            trackingInfo.put("sendTime", String.valueOf(System.currentTimeMillis()));
            
            redisTemplate.opsForHash().putAll(PENDING_TRACKING_KEY + commandId, trackingInfo);
            redisTemplate.expire(PENDING_TRACKING_KEY + commandId, COMMAND_TIMEOUT_SECONDS * 3, TimeUnit.SECONDS);

            // 实际发送MQTT指令
            sendMqttCommand(vehicleId, commandType, commandData);

            log.info("指令已下发: commandId={}, vehicleId={}, type={}", commandId, vehicleId, commandType);

            return commandId;

        } catch (Exception e) {
            log.error("下发指令失败: vehicleId={}, type={}", vehicleId, commandType, e);
            return null;
        }
    }

    /**
     * 处理设备ACK确认
     * 
     * @param commandId 指令ID
     * @param status 确认状态（received/handling/resolved）
     * @param message 确认消息
     */
    public void handleCommandAck(String commandId, String status, String message) {
        try {
            Map<String, String> trackingInfo = new HashMap<>();
            trackingInfo.put("status", status.toUpperCase());
            trackingInfo.put("ackTime", String.valueOf(System.currentTimeMillis()));
            trackingInfo.put("ackMessage", message != null ? message : "");

            redisTemplate.opsForHash().putAll(PENDING_TRACKING_KEY + commandId, trackingInfo);

            log.info("指令已确认: commandId={}, status={}", commandId, status);

        } catch (Exception e) {
            log.error("处理指令确认失败: commandId={}", commandId, e);
        }
    }

    /**
     * 定时检查超时指令并重试
     */
    @Scheduled(fixedDelay = 5000)  // 每5秒检查一次
    public void checkTimeoutAndRetry() {
        try {
            ensureConsumerGroup();

            Consumer consumer = Consumer.from(COMMAND_GROUP_NAME, COMMAND_CONSUMER_NAME);
            
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(50)
                .block(Duration.ZERO);

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(COMMAND_STREAM_KEY, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    Map<Object, Object> value = record.getValue();
                    
                    String commandId = (String) value.get("commandId");
                    String status = (String) value.get("status");
                    
                    // 如果已经确认，直接ACK
                    if ("CONFIRMED".equals(status) || "RESOLVED".equals(status)) {
                        redisTemplate.opsForStream().acknowledge(COMMAND_STREAM_KEY, COMMAND_GROUP_NAME, record.getId());
                        continue;
                    }

                    // 检查是否超时
                    if (isCommandTimeout(commandId)) {
                        int retryCount = Integer.parseInt((String) value.get("retryCount"));
                        
                        if (retryCount < MAX_RETRY_COUNT) {
                            // 重试
                            retryCommand(record, retryCount + 1);
                        } else {
                            // 超过最大重试次数，标记为失败
                            markCommandFailed(commandId);
                            redisTemplate.opsForStream().acknowledge(COMMAND_STREAM_KEY, COMMAND_GROUP_NAME, record.getId());
                        }
                    }

                } catch (Exception e) {
                    log.error("检查指令超时失败: messageId={}", record.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("检查超时指令失败", e);
        }
    }

    /**
     * 重试指令
     */
    private void retryCommand(MapRecord<String, Object, Object> record, int newRetryCount) {
        try {
            Map<Object, Object> value = record.getValue();
            
            String commandId = (String) value.get("commandId");
            Long vehicleId = Long.parseLong((String) value.get("vehicleId"));
            String commandType = (String) value.get("commandType");
            String commandDataStr = (String) value.get("commandData");

            Map<String, Object> commandData = objectMapper.readValue(commandDataStr, Map.class);

            // 更新重试次数
            redisTemplate.opsForHash().put(PENDING_TRACKING_KEY + commandId, "retryCount", String.valueOf(newRetryCount));
            redisTemplate.opsForHash().put(PENDING_TRACKING_KEY + commandId, "lastRetryTime", String.valueOf(System.currentTimeMillis()));

            // 重新发送MQTT指令
            sendMqttCommand(vehicleId, commandType, commandData);

            log.warn("指令重试: commandId={}, retryCount={}/{}", commandId, newRetryCount, MAX_RETRY_COUNT);

        } catch (Exception e) {
            log.error("重试指令失败: commandId={}", record.getId(), e);
        }
    }

    /**
     * 标记指令失败
     */
    private void markCommandFailed(String commandId) {
        try {
            redisTemplate.opsForHash().put(PENDING_TRACKING_KEY + commandId, "status", "FAILED");
            redisTemplate.opsForHash().put(PENDING_TRACKING_KEY + commandId, "failTime", String.valueOf(System.currentTimeMillis()));

            log.error("指令失败（超过最大重试次数）: commandId={}", commandId);

        } catch (Exception e) {
            log.error("标记指令失败异常: commandId={}", commandId, e);
        }
    }

    /**
     * 检查指令是否超时
     */
    private boolean isCommandTimeout(String commandId) {
        try {
            Object sendTimeObj = redisTemplate.opsForHash().get(PENDING_TRACKING_KEY + commandId, "sendTime");
            if (sendTimeObj == null) {
                return false;
            }

            long sendTime = Long.parseLong(sendTimeObj.toString());
            long elapsed = (System.currentTimeMillis() - sendTime) / 1000;

            return elapsed > COMMAND_TIMEOUT_SECONDS;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 发送MQTT指令
     */
    private void sendMqttCommand(Long vehicleId, String commandType, Map<String, Object> commandData) {
        try {
            String topic = "vehicle/" + vehicleId + "/command";
            String payload = objectMapper.writeValueAsString(commandData);
            
            mqttPublishService.publish(topic, payload);
            
            log.debug("MQTT指令已发送: vehicleId={}, type={}", vehicleId, commandType);

        } catch (Exception e) {
            log.error("发送MQTT指令失败: vehicleId={}, type={}", vehicleId, commandType, e);
        }
    }

    /**
     * 确保消费者组存在
     */
    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(COMMAND_STREAM_KEY, ReadOffset.latest(), COMMAND_GROUP_NAME);
        } catch (Exception e) {
            // 组已存在，忽略
        }
    }

    /**
     * 生成指令ID
     */
    private String generateCommandId() {
        return "CMD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * 查询指令状态
     */
    public Map<String, String> getCommandStatus(String commandId) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(PENDING_TRACKING_KEY + commandId);
            Map<String, String> result = new HashMap<>();
            
            // 转换类型：Map<Object,Object> -> Map<String,String>
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("查询指令状态失败: commandId={}", commandId, e);
            return new HashMap<>();
        }
    }

    /**
     * 获取Pending指令数量
     */
    public long getPendingCommandCount() {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                .pending(COMMAND_STREAM_KEY, COMMAND_GROUP_NAME);
            return summary != null ? summary.getTotalPendingMessages() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 监控统计
     */
    @Scheduled(fixedDelay = 30000)
    public void printStats() {
        Long streamLength = redisTemplate.opsForStream().size(COMMAND_STREAM_KEY);
        long pendingCount = getPendingCommandCount();
        
        log.info("指令Stream统计: streamLength={}, pendingCount={}", streamLength, pendingCount);
        
        if (pendingCount > 100) {
            log.warn("⚠️ 指令积压严重: pendingCount={}", pendingCount);
        }
    }

    /**
     * 指令跟踪信息
     */
    @Data
    public static class CommandTrackingInfo {
        private String commandId;
        private String status;
        private int retryCount;
        private long sendTime;
        private long ackTime;
        private String ackMessage;
    }
}
