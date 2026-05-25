package com.fence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 告警消息分发服务（基于Redis Stream）
 * 
 * 优化场景：多消费者场景 - 告警需要推送到多个渠道
 * 
 * 问题：
 * - 当前同步推送所有渠道，耗时长
 * - 某个渠道失败会影响其他渠道
 * - 无法独立扩展各个渠道
 * 
 * 解决方案：
 * - 告警产生后发送到Stream
 * - 多个消费者组独立消费（WebSocket、短信、邮件、APP推送）
 * - 各渠道互不影响，可独立重试
 */
@Slf4j
@Service
public class AlarmDistributionStreamService {

    private static final String ALARM_STREAM_KEY = "alarm:distribution:stream";
    
    // 不同的消费者组代表不同的通知渠道
    private static final String WEBSOCKET_GROUP = "alarm-websocket-group";
    private static final String SMS_GROUP = "alarm-sms-group";
    private static final String EMAIL_GROUP = "alarm-email-group";
    private static final String APP_PUSH_GROUP = "alarm-app-push-group";

    private static final long MAX_STREAM_LENGTH = 50000;  // 最多保留5万条

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发布告警事件到Stream
     * 
     * @param alarmId 告警ID
     * @param vehicleId 车辆ID
     * @param alarmType 告警类型
     * @param content 告警内容
     */
    public void publishAlarmEvent(Long alarmId, Long vehicleId, String alarmType, String content) {
        try {
            Map<String, String> messageBody = new HashMap<>();
            messageBody.put("alarmId", String.valueOf(alarmId));
            messageBody.put("vehicleId", String.valueOf(vehicleId));
            messageBody.put("alarmType", alarmType);
            messageBody.put("content", content);
            messageBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
            messageBody.put("level", determineAlarmLevel(alarmType));

            // XADD 添加消息
            redisTemplate.opsForStream().add(ALARM_STREAM_KEY, messageBody);

            // 限制队列长度
            redisTemplate.opsForStream().trim(ALARM_STREAM_KEY, MAX_STREAM_LENGTH);

            log.info("告警事件已发布: alarmId={}, type={}", alarmId, alarmType);

        } catch (Exception e) {
            log.error("发布告警事件失败: alarmId={}", alarmId, e);
        }
    }

    /**
     * WebSocket消费者 - 实时推送前端
     */
    @Scheduled(fixedDelay = 100)
    public void consumeForWebSocket() {
        consumeAlarm(WEBSOCKET_GROUP, "websocket-consumer-1", this::sendToWebSocket);
    }

    /**
     * 短信消费者 - 重要告警发送短信
     */
    @Scheduled(fixedDelay = 1000)
    public void consumeForSms() {
        consumeAlarm(SMS_GROUP, "sms-consumer-1", this::sendSmsNotification);
    }

    /**
     * 邮件消费者 - 汇总告警发送邮件
     */
    @Scheduled(fixedDelay = 5000)
    public void consumeForEmail() {
        consumeAlarm(EMAIL_GROUP, "email-consumer-1", this::sendEmailNotification);
    }

    /**
     * APP推送消费者 - 推送移动端
     */
    @Scheduled(fixedDelay = 500)
    public void consumeForAppPush() {
        consumeAlarm(APP_PUSH_GROUP, "app-push-consumer-1", this::sendAppPush);
    }

    /**
     * 通用的告警消费方法
     */
    private void consumeAlarm(String groupName, String consumerName, AlarmHandler handler) {
        try {
            ensureConsumerGroup(groupName);

            Consumer consumer = Consumer.from(groupName, consumerName);
            
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(10)
                .block(Duration.ZERO);

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(ALARM_STREAM_KEY, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record : records) {
                try {
                    Map<Object, Object> value = record.getValue();
                    
                    Long alarmId = Long.parseLong((String) value.get("alarmId"));
                    Long vehicleId = Long.parseLong((String) value.get("vehicleId"));
                    String alarmType = (String) value.get("alarmType");
                    String content = (String) value.get("content");

                    // 调用具体的处理逻辑
                    handler.handle(alarmId, vehicleId, alarmType, content);

                    // ACK确认
                    redisTemplate.opsForStream().acknowledge(ALARM_STREAM_KEY, groupName, record.getId());

                    log.debug("告警已处理: group={}, alarmId={}", groupName, alarmId);

                } catch (Exception e) {
                    log.error("处理告警失败: group={}, messageId={}", groupName, record.getId(), e);
                    // 不ACK，消息会进入Pending状态
                }
            }

        } catch (Exception e) {
            log.error("消费告警失败: group={}", groupName, e);
        }
    }

    /**
     * 确保消费者组存在
     */
    private void ensureConsumerGroup(String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(ALARM_STREAM_KEY, ReadOffset.latest(), groupName);
        } catch (Exception e) {
            // 组已存在，忽略
        }
    }

    /**
     * 发送到WebSocket
     */
    private void sendToWebSocket(Long alarmId, Long vehicleId, String alarmType, String content) {
        // TODO: 调用WebSocket服务推送
        log.info("[WebSocket] 推送告警: alarmId={}, vehicleId={}, type={}", alarmId, vehicleId, alarmType);
    }

    /**
     * 发送短信通知
     */
    private void sendSmsNotification(Long alarmId, Long vehicleId, String alarmType, String content) {
        // 只有高等级告警才发送短信
        String level = determineAlarmLevel(alarmType);
        if (!"HIGH".equals(level)) {
            return;
        }

        // TODO: 调用短信服务
        log.info("[SMS] 发送短信告警: alarmId={}, vehicleId={}, type={}", alarmId, vehicleId, alarmType);
    }

    /**
     * 发送邮件通知
     */
    private void sendEmailNotification(Long alarmId, Long vehicleId, String alarmType, String content) {
        // TODO: 调用邮件服务（可以批量发送）
        log.info("[Email] 发送邮件告警: alarmId={}, vehicleId={}, type={}", alarmId, vehicleId, alarmType);
    }

    /**
     * 发送APP推送
     */
    private void sendAppPush(Long alarmId, Long vehicleId, String alarmType, String content) {
        // TODO: 调用APP推送服务
        log.info("[APP Push] 推送告警: alarmId={}, vehicleId={}, type={}", alarmId, vehicleId, alarmType);
    }

    /**
     * 确定告警等级
     */
    private String determineAlarmLevel(String alarmType) {
        switch (alarmType) {
            case "OUT_FENCE":
            case "OFFLINE":
                return "HIGH";
            case "SPEED_OVERDUE":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    /**
     * 获取各渠道的Pending数量
     */
    public Map<String, Long> getPendingCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("websocket", getPendingCount(WEBSOCKET_GROUP));
        counts.put("sms", getPendingCount(SMS_GROUP));
        counts.put("email", getPendingCount(EMAIL_GROUP));
        counts.put("app_push", getPendingCount(APP_PUSH_GROUP));
        return counts;
    }

    private long getPendingCount(String groupName) {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                .pending(ALARM_STREAM_KEY, groupName);
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
        Long streamLength = redisTemplate.opsForStream().size(ALARM_STREAM_KEY);
        Map<String, Long> pendingCounts = getPendingCounts();
        
        log.info("告警Stream统计: streamLength={}, pending={}", streamLength, pendingCounts);
    }

    /**
     * 告警处理器接口
     */
    @FunctionalInterface
    private interface AlarmHandler {
        void handle(Long alarmId, Long vehicleId, String alarmType, String content);
    }
}
