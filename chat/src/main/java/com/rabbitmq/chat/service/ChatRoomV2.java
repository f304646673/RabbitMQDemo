package com.rabbitmq.chat.service;

import java.util.Collections;
import java.util.Date;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatRoomV2 {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void createChatRoom(String admin, String roomName) {
        createChatRoom(roomName);
    }

    private void createChatRoom(String roomName) {
        rabbitTemplate.execute(action -> {
            action.exchangeDeclare(roomName, "fanout", false, true, null);
            action.queueDeclare(roomName, true, false, false,
                Collections.singletonMap("x-queue-type", "stream"));
            action.queueBind(roomName, roomName, "");
            return null;
        });
    }

    public void send(String username, String roomName, String message) {
        Message msg = MessageBuilder.withBody(message.getBytes())
            .setHeader("username", username)
            .build();
        rabbitTemplate.send(roomName, "", msg);
    }

    public Flux<String> receive(String username, String roomName) {
        return Flux.create(emitter -> {
            rabbitTemplate.execute(channel -> {
                channel.basicQos(100);
                Date timestamp = new Date(System.currentTimeMillis());
                channel.basicConsume(roomName, false, username,
                    false, true,
                        Collections.singletonMap("x-stream-offset", timestamp),
                        (consumerTag, message) -> {
                            String senderOfMessage = message.getProperties().getHeaders().get("username").toString();
                            String show = "You Said: ";
                            if (!senderOfMessage.equals(username)) {
                                show = senderOfMessage + " Said: ";
                            }
                            show += new String(message.getBody());
                            System.out.println(show);
                            emitter.next(show);
                            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                        },
                        consumerTag -> { }
                );
                return null;
            });
        });
    }

}
