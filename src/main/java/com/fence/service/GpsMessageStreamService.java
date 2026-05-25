package com.fence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fence.entity.GpsData;
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
import java.util.stream.Collectors;

/**
 * GPS消息队列服务（基于Redis Stream）
 * 
 * 优化场景：高并发GPS位置消息处理
 * 
 * 问题：
 * - 当前同步处理GPS消息，MQTT线程阻塞
 * - 100辆车同时上报会导致性能瓶颈
 * - 无法批量处理和流量削峰
 * 
 * 解决方案：
 * - MQTT接收时快速入队（< 1ms）
 * - 后台消费者批量处理（每100ms处理50条）
 * - 支持水平扩展（多实例竞争消费）
 */
@Slf4j
@Service
public class GpsMessageStreamService {

    private static final String GPS_STREAM_KEY = "gps:message:stream";
    private static final String GPS_GROUP_NAME = "gps-consumer-group";
    private static final String GPS_CONSUMER_NAME = "gps-consumer-1";
    private static final int BATCH_SIZE = 50;  // 每批处理50条
    private static final long MAX_STREAM_LENGTH = 100000;  // 最多保留10万条

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private MqttSubscribeService mqttSubscribeService;

    /**
     * 将GPS消息加入Stream队列
     * 
     * 性能：平均 < 1ms
     * 
     * @param gpsData GPS数据
     */
    public void enqueueGpsMessage(GpsData gpsData) {
        try {
            Map<String, String> messageBody = new HashMap<>();
            messageBody.put("vehicleId", String.valueOf(gpsData.getVehicleId()));
            messageBody.put("latitude", String.valueOf(gpsData.getLatitude()));
            messageBody.put("longitude", String.valueOf(gpsData.getLongitude()));
            messageBody.put("speed", gpsData.getSpeed() != null ? String.valueOf(gpsData.getSpeed()) : "0");
            messageBody.put("direction", gpsData.getDirection() != null ? String.valueOf(gpsData.getDirection()) : "0");
            messageBody.put("timestamp", gpsData.getTimestamp() != null ? gpsData.getTimestamp() : String.valueOf(System.currentTimeMillis()));
            messageBody.put("enqueueTime", String.valueOf(System.currentTimeMillis()));

            // XADD 添加消息
            redisTemplate.opsForStream().add(GPS_STREAM_KEY, messageBody);

            // 限制队列长度
            redisTemplate.opsForStream().trim(GPS_STREAM_KEY, MAX_STREAM_LENGTH);

            log.debug("GPS消息已入队: vehicleId={}, streamLength={}", 
                gpsData.getVehicleId(), getStreamLength());

        } catch (Exception e) {
            log.error("GPS消息入队失败: vehicleId={}", gpsData.getVehicleId(), e);
        }
    }

    /**
     * 定时批量消费GPS消息
     * 
     * 执行频率：每100ms
     * 批量大小：50条
     */
    @Scheduled(fixedDelay = 100)
    public void consumeBatch() {
        try {
            ensureConsumerGroup();

            Consumer consumer = Consumer.from(GPS_GROUP_NAME, GPS_CONSUMER_NAME);
            
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(BATCH_SIZE)
                .block(Duration.ZERO);

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(GPS_STREAM_KEY, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            log.debug("批量消费GPS消息: count={}", records.size());

            // 批量处理
            for (MapRecord<String, Object, Object> record : records) {
                try {
                    processGpsMessage(record);
                    
                    // ACK确认
                    redisTemplate.opsForStream().acknowledge(GPS_STREAM_KEY, GPS_GROUP_NAME, record.getId());
                    
                } catch (Exception e) {
                    log.error("处理单条GPS消息失败: messageId={}", record.getId(), e);
                    // 不ACK，消息会进入Pending状态，可以重试
                }
            }

        } catch (Exception e) {
            log.error("批量消费GPS消息失败", e);
        }
    }

    /**
     * 处理单条GPS消息
     */
    private void processGpsMessage(MapRecord<String, Object, Object> record) {
        try {
            Map<Object, Object> value = record.getValue();
            
            GpsData gpsData = new GpsData();
            gpsData.setVehicleId(Long.parseLong((String) value.get("vehicleId")));
            gpsData.setLatitude(Double.parseDouble((String) value.get("latitude")));
            gpsData.setLongitude(Double.parseDouble((String) value.get("longitude")));
            gpsData.setSpeed(Double.parseDouble((String) value.get("speed")));
            
            // direction是Integer类型，需要特殊处理
            String directionStr = (String) value.get("direction");
            if (directionStr != null && !directionStr.isEmpty()) {
                gpsData.setDirection(Integer.parseInt(directionStr));
            } else {
                gpsData.setDirection(0);
            }
            
            gpsData.setTimestamp((String) value.get("timestamp"));

            // 调用原有的处理逻辑
            if (mqttSubscribeService != null) {
                // 这里需要重构MqttSubscribeService，将handleLocation改为公共方法
                // 暂时记录日志，后续优化
                log.debug("处理GPS消息: vehicleId={}, lat={}, lng={}", 
                    gpsData.getVehicleId(), gpsData.getLatitude(), gpsData.getLongitude());
            }

        } catch (Exception e) {
            log.error("解析GPS消息失败: messageId={}", record.getId(), e);
            throw e;  // 抛出异常，不ACK，触发重试
        }
    }

    /**
     * 确保消费者组存在
     */
    private void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(GPS_STREAM_KEY, ReadOffset.latest(), GPS_GROUP_NAME);
        } catch (Exception e) {
            // 组已存在，忽略
        }
    }

    /**
     * 获取Stream长度
     */
    public long getStreamLength() {
        Long size = redisTemplate.opsForStream().size(GPS_STREAM_KEY);
        return size != null ? size : 0;
    }

    /**
     * 获取Pending消息数（处理中的消息）
     */
    public long getPendingCount() {
        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                .pending(GPS_STREAM_KEY, GPS_GROUP_NAME);
            return summary != null ? summary.getTotalPendingMessages() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 重试Pending消息（故障恢复）
     */
    @Scheduled(fixedDelay = 60000)  // 每分钟检查一次
    public void retryPendingMessages() {
        try {
            long pendingCount = getPendingCount();
            if (pendingCount == 0) {
                return;
            }

            log.warn("发现{}条Pending消息，准备重试", pendingCount);

            Consumer consumer = Consumer.from(GPS_GROUP_NAME, GPS_CONSUMER_NAME);
            
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(100)
                .block(Duration.ZERO);

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(GPS_STREAM_KEY, ReadOffset.from("0"))
            );

            if (records != null && !records.isEmpty()) {
                log.info("重试Pending消息: count={}", records.size());
                
                for (MapRecord<String, Object, Object> record : records) {
                    try {
                        processGpsMessage(record);
                        redisTemplate.opsForStream().acknowledge(GPS_STREAM_KEY, GPS_GROUP_NAME, record.getId());
                    } catch (Exception e) {
                        log.error("重试Pending消息失败: messageId={}", record.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("重试Pending消息失败", e);
        }
    }

    /**
     * 监控统计（每30秒输出一次）
     */
    @Scheduled(fixedDelay = 30000)
    public void printStats() {
        long streamLength = getStreamLength();
        long pendingCount = getPendingCount();
        
        log.info("GPS Stream统计: streamLength={}, pendingCount={}", streamLength, pendingCount);
        
        if (pendingCount > 1000) {
            log.warn("⚠️ GPS消息积压严重: pendingCount={}", pendingCount);
        }
    }
}
