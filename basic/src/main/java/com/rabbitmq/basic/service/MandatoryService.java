package com.rabbitmq.basic.service;

import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class MandatoryService {
    @Resource(name = "rabbitTemplateWithMandatory")
    private RabbitTemplate rabbitTemplateWithMandatory;

    public void send(String exchangeName, String routingKey, String message) {
        String msgId = UUID.randomUUID().toString();
        Message msg = MessageBuilder.withBody(message.getBytes())
                .setContentType("text/plain")
                .setCorrelationId(msgId)
                .setMessageId(msgId)
                .build();

        CorrelationData correlationData = new CorrelationData(msgId);
        rabbitTemplateWithMandatory.convertAndSend(exchangeName, routingKey, msg, correlationData);
    }
}
