package com.fence.service;
//wzj
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 离线消息队列服务
 *
 * 为什么叫"队列"？
 * → 消息像排队一样，先进先出
 * → 不会丢失，也不会乱序
 */
@Slf4j
@Service
public class OfflineMessageQueue {

    private static final String QUEUE_PREFIX = "offline:queue:";
    private static final String HASH_PREFIX = "offline:hash:";
    private static final long MESSAGE_EXPIRE_DAYS = 7; // 消息保留7天

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 添加离线消息到队列
     *
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void addMessage(String userId, String message) {
        String queueKey = QUEUE_PREFIX + userId;
        String hashKey = HASH_PREFIX + userId;

        // 生成消息ID
        String messageId = generateMessageId();

        // 存储消息详情（Hash）
        redisTemplate.opsForHash().put(hashKey, messageId, message);
        redisTemplate.expire(hashKey, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);

        // 加入队列（List）
        redisTemplate.opsForList().rightPush(queueKey, messageId);
        redisTemplate.expire(queueKey, MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);

        log.info("添加离线消息: userId={}, messageId={}, 队列长度={}", userId, messageId, getQueueLength(userId));
    }

    /**
     * 获取并移除离线消息
     *
     * @param userId 用户ID
     * @param maxCount 最多获取多少条
     * @return 消息列表
     */
    public List<String> popMessages(String userId, int maxCount) {
        String queueKey = QUEUE_PREFIX + userId;
        String hashKey = HASH_PREFIX + userId;

        List<String> messages = new ArrayList<>();

        for (int i = 0; i < maxCount; i++) {
            // 从队列取出消息ID
            Object messageId = redisTemplate.opsForList().leftPop(queueKey);
            if (messageId == null) {
                break;
            }

            // 从Hash获取消息内容
            Object message = redisTemplate.opsForHash().get(hashKey, messageId);
            if (message != null) {
                messages.add(message.toString());
                // 删除Hash中的记录
                redisTemplate.opsForHash().delete(hashKey, messageId);
            }
        }

        return messages;
    }

    /**
     * 获取队列长度（未读消息数）
     */
    public long getQueueLength(String userId) {
        String queueKey = QUEUE_PREFIX + userId;
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return System.currentTimeMillis() + ":" + Thread.currentThread().getId() + ":" + (int)(Math.random() * 1000);
    }
}
