package com.rabbitmq.chat.service;

import java.util.AbstractMap.SimpleEntry;
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
public class Core {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private ConnectionFactory connectionFactory;

    private final ReentrantLock lock = new ReentrantLock();
    final private Map<String, SimpleMessageListenerContainer> listeners = new java.util.HashMap<>();
    final String exchangeName = "Core";

    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
        createExchange(exchangeName);
    }

    public Flux<String> Login(String username) {
        createExclusiveQueue(username);
        createBanding(exchangeName, username, username);
        return Flux.create(emitter -> {
           SimpleMessageListenerContainer container = getListener(username, (Message message) -> {
               String msg = new String(message.getBody());
               System.out.println("Received message: " + msg);
               emitter.next(msg);
           });
           container.start();
       });
    }

    public void invite(String fromUsername, String toUsername) {
        rabbitTemplate.convertAndSend(exchangeName, toUsername, fromUsername + " invites you to chat");
    }

    public void accept(String fromUsername, String toUsername) {
        rabbitTemplate.convertAndSend(exchangeName, fromUsername, toUsername + " accepts your invitation");
    }

    public Map.Entry<String, String> createChatRoom(String fromUsername, String toUsername) {
        String chatRoomName = fromUsername + "-" + toUsername;
        createExchange(chatRoomName);
        String queueName = "queue-" + fromUsername + "-" + toUsername;
        createExclusiveQueue(queueName);
        String routingKey = "to." + queueName;
        createBanding(chatRoomName, queueName, routingKey);
        return new SimpleEntry<>(chatRoomName, routingKey);
    }

    public void sendNotify(String username, String msg) {
        rabbitTemplate.convertAndSend(exchangeName, username, msg);
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

    private void createExchange(String exchangeName) {
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare(exchangeName, "direct", false, true, null);
            return null;
        });
    }

    private void createBanding(String exchangeName, String queueName, String routingKey) {
        rabbitTemplate.execute(channel -> {
            channel.queueBind(queueName, exchangeName, routingKey);
            return null;
        });
    }

    private void createExclusiveQueue(String username) {
        // Create a queue with the username as the name
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(username, false, true, false, null);
            return null;
        });
    }
}
