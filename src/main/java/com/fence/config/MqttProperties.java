package com.fence.config;
//wzj
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT配置属性类
 *
 * 绑定 application.yml 中的 mqtt.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "electronic-fence-server";
    private String username = "";
    private String password = "";
}
