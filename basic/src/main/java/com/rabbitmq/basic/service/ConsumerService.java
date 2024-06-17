package com.rabbitmq.basic.service;

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

@Service
public class ConsumerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ConnectionFactory connectionFactory;

    private final ReentrantLock lock = new ReentrantLock();
    private Map<String, SimpleMessageListenerContainer> listeners = new java.util.HashMap<>();

    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
    }

    public void startListening(String queueName) {
        SimpleMessageListenerContainer container = getListener(queueName, new RBMQMessageListener());
        container.start();
        System.out.println("Listening to " + queueName);
    }
    
    public void stopListening(String queueName) {
        SimpleMessageListenerContainer container = getListener(queueName, null);
        container.stop();
        System.out.println("Stopped listening to " + queueName);
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

    private class RBMQMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            System.out.println("Received message: " + new String(message.getBody()));
        }
    }

}
