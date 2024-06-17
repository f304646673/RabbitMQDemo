package com.rabbitmq.chat.service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import reactor.core.publisher.Flux;

@Service
public class ChatRoom {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private ConnectionFactory connectionFactory;

    @Data
    private class ChatRoomInfo {
        private String exchange;
        private Map<String, String> usernameToQueuename;
    }

    private Map<String, ChatRoomInfo> chatRooms = new java.util.HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();   
    
    @PostConstruct
    public void init() {
        connectionFactory = rabbitTemplate.getConnectionFactory();
    }

    public Flux<String> invite(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            createChatRoom(fromUsername, toUsername);
        }
        return talk(chatRoomName, fromUsername);
    }

    public Flux<String> accept(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        return talk(chatRoomName, toUsername);
    }

    private Flux<String> talk(String chatRoomName, String username) {
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            throw new IllegalArgumentException("Chat room not found");
        }
        String queueName = chatRoomInfo.getUsernameToQueuename().get(username);
        return Flux.create(emitter -> {
            SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer();
            listener.setConnectionFactory(connectionFactory);
            listener.setQueueNames(queueName);
            listener.setMessageListener((Message message) -> {
                String msg = new String(message.getBody());
                System.out.println(username + " received message: " + msg);
                emitter.next(msg);
            });
            listener.start();
        });
    }
    
    private void createChatRoom(String fromUsername, String toUsername) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        String exchangeName = chatRoomName;
        String fromQueueName = "queue-" + fromUsername + "-" + toUsername;
        String toQueueName = "queue-" + toUsername + "-" + fromUsername;
        
        rabbitTemplate.execute(action -> {
            action.exchangeDeclare(exchangeName, "fanout", false, true, null);
            action.queueDeclare(fromQueueName, false, true, false, null);
            action.queueDeclare(toQueueName, false, true, false, null);
            action.queueBind(fromQueueName, exchangeName, "");
            action.queueBind(toQueueName, exchangeName, "");
            return null;
        });

        lock.lock();
        try {
            ChatRoomInfo chatRoomInfo = new ChatRoomInfo();
            chatRoomInfo.setExchange(exchangeName);
            chatRoomInfo.setUsernameToQueuename(Map.of(fromUsername, fromQueueName, toUsername, toQueueName));
            chatRooms.put(chatRoomName, chatRoomInfo);
        } finally {
            lock.unlock();
        }
    }

    public void chat(String fromUsername, String toUsername, String message) {
        String chatRoomName = getChatRoomName(fromUsername, toUsername);
        ChatRoomInfo chatRoomInfo = chatRooms.get(chatRoomName);
        if (chatRoomInfo == null) {
            chatRoomName = getChatRoomName(toUsername, fromUsername);
            chatRoomInfo = chatRooms.get(chatRoomName);
        }
        if (chatRoomInfo == null) {
            throw new IllegalArgumentException("Chat room not found");
        }
        rabbitTemplate.convertAndSend(chatRoomInfo.getExchange(), "", fromUsername + ": " + message);
    }

    private String getChatRoomName(String fromUsername, String toUsername) {
        return fromUsername + "-" + toUsername + "-chat-room";
    }

    
}
