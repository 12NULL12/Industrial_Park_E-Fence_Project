package com.fence.websocket;
//wzj
import com.fence.service.OfflineMessageQueue;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务（集成离线消息）
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketWithOffline {

    private static Map<String, Session> onlineUsers = new ConcurrentHashMap<>();

    // 注入会失效，用其他方式
    private static OfflineMessageQueue offlineMessageQueue;

    @Autowired
    public void setOfflineMessageQueue(OfflineMessageQueue queue) {
        WebSocketWithOffline.offlineMessageQueue = queue;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        onlineUsers.put(userId, session);
        log.info("用户 {} 上线", userId);

        // 上线后，发送离线消息
        sendOfflineMessages(userId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        onlineUsers.remove(userId);
        log.info("用户 {} 下线", userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equals(message)) {
            sendMessage(session, "pong");
        }
    }

    /**
     * 用户上线时，发送离线消息
     */
    private void sendOfflineMessages(String userId) {
        if (offlineMessageQueue == null) {
            return;
        }

        long count = offlineMessageQueue.getQueueLength(userId);
        if (count > 0) {
            log.info("用户 {} 有 {} 条离线消息", userId, count);

            // 取出所有离线消息并发送
            offlineMessageQueue.popMessages(userId, (int) count).forEach(msg -> {
                sendToUser(userId, msg);
            });
        }
    }

    /**
     * 发送消息（在线直接发，离线加入队列）
     */
    public static void sendToUser(String userId, String message) {
        Session session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        } else {
            // 用户不在线，加入离线队列
            if (offlineMessageQueue != null) {
                offlineMessageQueue.addMessage(userId, message);
                log.info("用户 {} 不在线，消息加入离线队列", userId);
            }
        }
    }

    private static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }
    public static void sendToAll(String message) {
        onlineUsers.forEach((userId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("广播消息失败", e);
                }
            }
        });
        log.debug("广播消息给 {} 个用户", onlineUsers.size());
    }
}
