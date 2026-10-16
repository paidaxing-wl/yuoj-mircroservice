package com.yupi.yuojbackendjudgeservice.rabbitmq;

import com.yupi.yuojbackendcommon.constant.RabbitMqConstant;
import com.yupi.yuojbackendjudgeservice.exception.NonRetryableJudgeException;
import com.yupi.yuojbackendjudgeservice.judge.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 判题任务消费者。
 * <p>
 * 正常结束由容器 ack；抛异常则不 ack，交给容器的重试与 DeadLetterRecoverer 兜底。
 *
 * @author lenovo
 */
@Slf4j
@Component
public class MyMessageConsumer {

    @Resource
    private JudgeService judgeService;

    @RabbitListener(queues = RabbitMqConstant.CODE_QUEUE, ackMode = "MANUAL")
    public void receiveMessage(String message,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到判题消息：message={}, deliveryTag={}", message, deliveryTag);

        long questionSubmitId;
        try {
            questionSubmitId = Long.parseLong(message);
        } catch (NumberFormatException e) {
            // 消息体非法属于永久性错误，重试多少次都一样，不能占用重试次数
            throw new NonRetryableJudgeException("判题消息内容非法：" + message, e);
        }

        try {
            judgeService.doJudge(questionSubmitId);
            log.info("判题任务处理完成：submitId={}", questionSubmitId);
        } catch (NonRetryableJudgeException e) {
            log.error("判题任务命中永久性异常，不再重试：submitId={}, 原因={}", questionSubmitId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("判题任务处理失败，将进入重试流程：submitId={}, 原因={}", questionSubmitId, e.getMessage(), e);
            throw e;
        }
    }
}
