package com.fence.service;
//wzj
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fence.entity.CommandPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * MQTT消息发送服务（成员B负责）
 *
 * 负责通过MQTT协议向设备下发指令
 * 使用Spring Integration的MessageChannel发送消息
 */
@Slf4j
@Service
public class MqttPublishService {

    @Autowired
    @Qualifier("mqttOutboundChannel")
    private MessageChannel mqttOutboundChannel;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送文本消息
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void publish(String topic, String message) {
        try {
            Message<String> mqttMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader("mqtt_topic", topic)
                    .setHeader("mqtt_qos", 1)
                    .setHeader("mqtt_retained", false)
                    .build();

            mqttOutboundChannel.send(mqttMessage);
            log.info("MQTT消息发送成功: topic={}, message={}", topic, message);

        } catch (Exception e) {
            log.error("MQTT消息发送失败: topic={}, message={}", topic, message, e);
        }
    }

    /**
     * 发送JSON对象
     *
     * @param topic 主题
     * @param payload 消息对象
     */
    public void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            publish(topic, json);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
        }
    }

    /**
     * 发送消息（指定QoS和Retained）
     *
     * @param topic 主题
     * @param message 消息内容
     * @param qos QoS级别
     * @param retained 是否保留
     */
    public void publish(String topic, String message, int qos, boolean retained) {
        try {
            Message<String> mqttMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader("mqtt_topic", topic)
                    .setHeader("mqtt_qos", qos)
                    .setHeader("mqtt_retained", retained)
                    .build();

            mqttOutboundChannel.send(mqttMessage);
            log.info("MQTT消息发送成功: topic={}, qos={}, retained={}, message={}",
                    topic, qos, retained, message);

        } catch (Exception e) {
            log.error("MQTT消息发送失败: topic={}, message={}", topic, message, e);
        }
    }

    /**
     * 发送远程指令给车辆
     *
     * @param vehicleId 车辆ID
     * @param commandPayload 指令内容
     */
    public void sendCommand(String vehicleId, CommandPayload commandPayload) {
        String topic = "vehicle/" + vehicleId + "/command";
        publish(topic, commandPayload);
    }
}
