package com.rabbitmq.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.chat.service.ChatRoom;
import com.rabbitmq.chat.service.Core;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private Core core;

    @Autowired
    private ChatRoom chatRoom;

    @PutMapping(value = "/invite", produces = "text/event-stream")
    public Flux<String> invite(@RequestParam String fromUsername, @RequestParam String toUsername) {
        core.invite(fromUsername, toUsername);
        return chatRoom.invite(fromUsername, toUsername);
    }

    @PutMapping(value = "/accept", produces = "text/event-stream")
    public Flux<String> accept(@RequestParam String fromUsername, @RequestParam String toUsername) {
        core.accept(fromUsername, toUsername);
        return chatRoom.accept(fromUsername, toUsername);
    }

    @PostMapping("/send")
    public void send(@RequestParam String fromUsername, @RequestParam String toUsername, @RequestParam String message) {
        chatRoom.chat(fromUsername, toUsername, message);
    }
    
}
