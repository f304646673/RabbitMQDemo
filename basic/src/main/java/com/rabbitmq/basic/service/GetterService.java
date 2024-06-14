package com.rabbitmq.basic.service;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetterService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public String getMessage() {
        Message msg = rabbitTemplate.receive("classic_queue");
        if (msg == null) {
            return "No message available";
        }
        return new String(msg.getBody());
    }
}
