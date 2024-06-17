package com.rabbitmq.chat.service;

import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Chat {
    @Autowired
    private Core core;

    public void invite(String fromUserName, String toUserName) {
        core.invite(fromUserName, toUserName);
    }

    public void accept(String fromUserName, String toUserName) {
        core.accept(fromUserName, toUserName);
        Entry<String,String> chatRoom = core.createChatRoom(fromUserName, toUserName);
        String msg = "Chat room created! exchange:" + chatRoom.getKey() + " and routingkey:" + chatRoom.getValue();
        core.sendNotify(fromUserName, msg);
        core.sendNotify(toUserName, msg);
    }

    public void send(String fromUserName, String toUserName, String message) {
        // core.send(fromUserName, toUserName, message);
    }

}