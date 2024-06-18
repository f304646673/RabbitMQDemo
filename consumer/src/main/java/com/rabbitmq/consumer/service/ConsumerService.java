package com.rabbitmq.consumer.service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Service
public class ConsumerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private ConnectionFactory connectionFactory;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, SimpleMessageListenerContainer> listeners = new java.util.HashMap<>();

    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
    }

    public Flux<String> listen(String queueName) {
       return Flux.create(emitter -> {
           SimpleMessageListenerContainer container = getListener(queueName, (Message message) -> {
               String msg = new String(message.getBody());
               System.out.println("listen function Received message: " + msg);
               emitter.next(msg);
           });
           container.start();
       });
    }

    private SimpleMessageListenerContainer getListener(String queueName, MessageListener messageListener) {
        lock.lock();
        try {
            SimpleMessageListenerContainer listener = listeners.get(queueName);
            if (listener == null && messageListener != null) {
                listener = new SimpleMessageListenerContainer();
                listener.setConnectionFactory(connectionFactory);
                listener.setQueueNames(queueName);
                listener.setMessageListener(messageListener);
                listeners.put(queueName, listener);
            }
            return listener;
        } finally {
            lock.unlock();
        }
    }
}
