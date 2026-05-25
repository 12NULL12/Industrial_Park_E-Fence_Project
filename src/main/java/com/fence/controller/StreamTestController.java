package com.fence.controller;

import com.fence.common.Result;
import com.fence.service.OfflineMessageStreamQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 离线消息测试控制器
 * 
 * 用于测试和演示 Redis Stream 的新特性
 */
@Slf4j
@RestController
@RequestMapping("/api/stream-test")
public class StreamTestController {

    @Autowired
    private OfflineMessageStreamQueue streamQueue;

    /**
     * 发送离线消息到Stream
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        if (userId == null || message == null) {
            return Result.error("userId和message不能为空");
        }

        try {
            streamQueue.addMessage(userId, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("queueLength", streamQueue.getQueueLength(userId));
            response.put("pendingCount", streamQueue.getPendingCount(userId));
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送测试消息
     */
    @PostMapping("/send-batch")
    public Result<Map<String, Object>> sendBatchMessages(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Integer count = (Integer) request.getOrDefault("count", 5);

        if (userId == null) {
            return Result.error("userId不能为空");
        }

        try {
            for (int i = 1; i <= count; i++) {
                String message = String.format("{\"type\":\"TEST\",\"content\":\"Stream测试消息 %d\",\"timestamp\":%d}", 
                    i, System.currentTimeMillis());
                streamQueue.addMessage(userId, message);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("sentCount", count);
            response.put("queueLength", streamQueue.getQueueLength(userId));
            response.put("message", String.format("已发送%d条消息", count));
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("批量发送消息失败", e);
            return Result.error("批量发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列状态
     */
    @GetMapping("/status/{userId}")
    public Result<Map<String, Object>> getQueueStatus(@PathVariable String userId) {
        try {
            long queueLength = streamQueue.getQueueLength(userId);
            long pendingCount = streamQueue.getPendingCount(userId);

            Map<String, Object> status = new HashMap<>();
            status.put("userId", userId);
            status.put("queueLength", queueLength);
            status.put("pendingCount", pendingCount);
            status.put("description", String.format(
                "队列中有%d条消息，其中%d条正在处理中", 
                queueLength, pendingCount
            ));

            return Result.success(status);
        } catch (Exception e) {
            log.error("获取队列状态失败", e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 消费离线消息（模拟WebSocket上线）
     */
    @PostMapping("/consume")
    public Result<Map<String, Object>> consumeMessages(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Integer maxCount = (Integer) request.getOrDefault("maxCount", 10);

        if (userId == null) {
            return Result.error("userId不能为空");
        }

        try {
            List<String> messages = streamQueue.popMessages(userId, maxCount);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("consumedCount", messages.size());
            response.put("remainingQueueLength", streamQueue.getQueueLength(userId));
            response.put("pendingCount", streamQueue.getPendingCount(userId));
            response.put("messages", messages);

            return Result.success(response);
        } catch (Exception e) {
            log.error("消费消息失败", e);
            return Result.error("消费消息失败: " + e.getMessage());
        }
    }

    /**
     * 重试Pending消息（故障恢复）
     */
    @PostMapping("/retry-pending")
    public Result<Map<String, Object>> retryPendingMessages(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Integer maxCount = (Integer) request.getOrDefault("maxCount", 10);

        if (userId == null) {
            return Result.error("userId不能为空");
        }

        try {
            List<String> messages = streamQueue.retryPendingMessages(userId, maxCount);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("retriedCount", messages.size());
            response.put("remainingPendingCount", streamQueue.getPendingCount(userId));
            response.put("messages", messages);

            return Result.success(response);
        } catch (Exception e) {
            log.error("重试Pending消息失败", e);
            return Result.error("重试失败: " + e.getMessage());
        }
    }

    /**
     * 清除用户Stream
     */
    @DeleteMapping("/clear/{userId}")
    public Result<String> clearUserStream(@PathVariable String userId) {
        try {
            streamQueue.clearUserStream(userId);
            return Result.success("已清除用户Stream");
        } catch (Exception e) {
            log.error("清除Stream失败", e);
            return Result.error("清除失败: " + e.getMessage());
        }
    }

    /**
     * 对比新旧方案的性能和功能
     */
    @GetMapping("/comparison")
    public Result<Map<String, Object>> getComparison() {
        Map<String, Object> comparison = new HashMap<>();
        
        Map<String, Object> oldScheme = new HashMap<>();
        oldScheme.put("name", "List + Hash 方案");
        oldScheme.put("features", List.of(
            "手动生成消息ID",
            "需要维护两个数据结构",
            "弹出即删除，无法回溯",
            "无ACK机制",
            "不支持消费者组"
        ));
        oldScheme.put("pros", List.of("实现简单", "适合小规模场景"));
        oldScheme.put("cons", List.of("可靠性低", "功能有限", "需手动管理"));

        Map<String, Object> newScheme = new HashMap<>();
        newScheme.put("name", "Redis Stream 方案");
        newScheme.put("features", List.of(
            "自动生成唯一消息ID",
            "单一Stream结构",
            "支持消息回溯",
            "内置ACK机制",
            "支持消费者组",
            "Pending状态追踪",
            "自动限制队列长度"
        ));
        newScheme.put("pros", List.of("高可靠性", "功能完整", "自动管理", "可扩展性强"));
        newScheme.put("cons", List.of("学习曲线稍陡"));

        comparison.put("oldScheme", oldScheme);
        comparison.put("newScheme", newScheme);
        comparison.put("recommendation", "推荐使用Redis Stream方案，特别是在需要高可靠性和扩展性的场景");

        return Result.success(comparison);
    }
}
