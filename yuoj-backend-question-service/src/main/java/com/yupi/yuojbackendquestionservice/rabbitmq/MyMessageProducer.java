package com.yupi.yuojbackendquestionservice.rabbitmq;

import com.yupi.yuojbackendcommon.constant.RabbitMqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 判题任务生产者。带上 CorrelationData 后，Broker 的确认结果可以在
 * RabbitMqProducerConfig 的 confirmCallback 里看到，消息丢失不再静默。
 *
 * @author lenovo
 */
@Slf4j
@Component
public class MyMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, String message) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
        log.info("已发送判题任务消息：exchange={}, routingKey={}, message={}, correlationId={}",
                exchange, routingKey, message, correlationData.getId());
    }

    public void sendJudgeTask(long questionSubmitId) {
        sendMessage(RabbitMqConstant.CODE_EXCHANGE, RabbitMqConstant.CODE_ROUTING_KEY,
                String.valueOf(questionSubmitId));
    }
}
