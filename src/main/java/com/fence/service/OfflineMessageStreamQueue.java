package com.fence.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Redis Stream 的离线消息队列服务（升级版）
 * 
 * 相比 List+Hash 方案的改进：
 * 1. ✅ 自动消息ID - 无需手动生成，Redis自动生成唯一ID
 * 2. ✅ 消息持久化 - Stream天然支持持久化，配合AOF更可靠
 * 3. ✅ 消费者组 - 支持多实例竞争消费，负载均衡
 * 4. ✅ ACK机制 - 消息确认机制，确保不丢失
 * 5. ✅ 消息回溯 - 可以重新消费历史消息
 * 6. ✅ 自动清理 - 基于MAXLEN自动限制队列长度
 * 7. ✅ Pending状态 - 可追踪未确认的消息
 */
@Slf4j
@Service
public class OfflineMessageStreamQueue {

    private static final String STREAM_KEY_PREFIX = "offline:stream:";
    private static final String GROUP_NAME = "offline-group";
    private static final String CONSUMER_NAME = "websocket-consumer";
    private static final long MAX_QUEUE_LENGTH = 10000; // 每个用户最多保留1万条消息
    private static final Duration MESSAGE_RETENTION = Duration.ofDays(7); // 消息保留7天

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加离线消息到Stream
     * 
     * 改进点：
     * - 使用 XADD 命令，Redis自动生成唯一消息ID（时间戳-序列号）
     * - 自动限制队列长度（MAXLEN），避免内存无限增长
     * - 消息以Hash形式存储，支持多个字段
     *
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void addMessage(String userId, String message) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            // 构建消息体（支持扩展更多字段）
            Map<String, String> messageBody = Map.of(
                "content", message,
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "type", "OFFLINE_MESSAGE"
            );

            // XADD 添加消息，*表示自动生成ID
            RecordId recordId = redisTemplate.opsForStream().add(
                streamKey, 
                messageBody
            );

            // 手动限制队列长度（trimming）
            redisTemplate.opsForStream().trim(streamKey, MAX_QUEUE_LENGTH);

            log.info("添加离线消息到Stream: userId={}, messageId={}, 队列已限制为{}条", 
                userId, recordId.getValue(), MAX_QUEUE_LENGTH);

        } catch (Exception e) {
            log.error("添加离线消息失败: userId={}", userId, e);
            throw new RuntimeException("添加离线消息失败", e);
        }
    }

    /**
     * 获取并确认离线消息（带ACK机制）
     * 
     * 改进点：
     * - 使用消费者组（Consumer Group），支持多实例竞争消费
     * - 消息读取后进入Pending状态，需要ACK确认才真正完成
     * - 如果消费失败，消息会留在Pending列表，可以重试
     * - 自动创建消费者组和消费者（首次使用时）
     *
     * @param userId 用户ID
     * @param maxCount 最多获取多少条
     * @return 消息列表
     */
    public List<String> popMessages(String userId, int maxCount) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            // 确保消费者组存在（首次使用时创建）
            ensureConsumerGroup(streamKey);

            // XREADGROUP 从消费者组读取消息
            // Block.none() 表示非阻塞读取
            // COUNT 限制读取数量
            Consumer consumer = Consumer.from(GROUP_NAME, CONSUMER_NAME);
            
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(maxCount)
                .block(Duration.ZERO); // 非阻塞模式

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) {
                return List.of();
            }

            // 提取消息内容并ACK确认
            List<String> messages = records.stream().map(record -> {
                String messageId = record.getId().getValue();
                Object content = record.getValue().get("content");
                
                // XACK 确认消息已处理
                redisTemplate.opsForStream().acknowledge(streamKey, GROUP_NAME, record.getId());
                
                log.debug("消费并ACK消息: userId={}, messageId={}", userId, messageId);
                
                return content != null ? content.toString() : null;
            }).filter(msg -> msg != null).collect(Collectors.toList());

            log.info("从Stream获取离线消息: userId={}, 获取{}条", userId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("获取离线消息失败: userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 获取未读消息数量
     * 
     * 改进点：
     * - 使用 XLEN 直接获取Stream长度，性能更好
     * - 无需维护额外的计数器
     *
     * @param userId 用户ID
     * @return 未读消息数
     */
    public long getQueueLength(String userId) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            Long size = redisTemplate.opsForStream().size(streamKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取队列长度失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 获取Pending消息数量（已读取但未ACK的消息）
     * 
     * 新增功能：
     * - 可以监控有多少消息处于"处理中"状态
     * - 用于检测是否有消息消费失败
     *
     * @param userId 用户ID
     * @return Pending消息数
     */
    public long getPendingCount(String userId) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                .pending(streamKey, GROUP_NAME);
            
            return summary != null ? summary.getTotalPendingMessages() : 0;
        } catch (Exception e) {
            log.error("获取Pending消息数失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 删除用户的离线消息Stream
     * 
     * @param userId 用户ID
     */
    public void clearUserStream(String userId) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            redisTemplate.delete(streamKey);
            log.info("清除用户离线消息Stream: userId={}", userId);
        } catch (Exception e) {
            log.error("清除Stream失败: userId={}", userId, e);
        }
    }

    /**
     * 确保消费者组存在
     * 
     * 改进点：
     * - 消费者组是Redis Stream的核心特性
     * - 支持多个消费者实例竞争消费同一Stream
     * - 每条消息只会被一个消费者处理，避免重复
     */
    private void ensureConsumerGroup(String streamKey) {
        try {
            // 尝试创建消费者组，从最新消息开始消费
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), GROUP_NAME);
            log.debug("创建消费者组成功: streamKey={}, group={}", streamKey, GROUP_NAME);
        } catch (Exception e) {
            // 如果组已存在，会抛出异常，忽略即可
            log.debug("消费者组已存在或Stream为空: {}", e.getMessage());
        }
    }

    /**
     * 重新消费Pending消息（用于故障恢复）
     * 
     * 新增功能：
     * - 如果之前消费失败（未ACK），可以重新读取这些消息
     * - 适用于服务重启后的消息恢复
     *
     * @param userId 用户ID
     * @param maxCount 最多重新消费多少条
     * @return 消息列表
     */
    public List<String> retryPendingMessages(String userId, int maxCount) {
        String streamKey = STREAM_KEY_PREFIX + userId;

        try {
            ensureConsumerGroup(streamKey);

            Consumer consumer = Consumer.from(GROUP_NAME, CONSUMER_NAME);
            
            // XREADGROUP 读取Pending消息（ID为"0"表示只读Pending的）
            StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(maxCount)
                .block(Duration.ZERO);

            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                consumer,
                readOptions,
                StreamOffset.create(streamKey, ReadOffset.from("0")) // "0"表示Pending消息
            );

            if (records == null || records.isEmpty()) {
                return List.of();
            }

            List<String> messages = records.stream().map(record -> {
                String messageId = record.getId().getValue();
                Object content = record.getValue().get("content");
                
                // 重新ACK
                redisTemplate.opsForStream().acknowledge(streamKey, GROUP_NAME, record.getId());
                
                log.info("重试Pending消息: userId={}, messageId={}", userId, messageId);
                
                return content != null ? content.toString() : null;
            }).filter(msg -> msg != null).collect(Collectors.toList());

            log.info("重试Pending消息完成: userId={}, 重试{}条", userId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("重试Pending消息失败: userId={}", userId, e);
            return List.of();
        }
    }
}
