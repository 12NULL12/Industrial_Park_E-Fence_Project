package com.fence.config;
//wzj
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置
 *
 * 为什么需要配置？
 * → 告诉Spring怎么创建WebSocket服务器
 */
@Configuration
public class WebSocketConfig {

    /**
     * 创建WebSocket服务器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
