package com.rabbitmq.chat.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.chat.service.ChatRoom;
import com.rabbitmq.chat.service.ChatRoomV2;
import com.rabbitmq.chat.service.Core;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private Core core;

    @Autowired
    private ChatRoom chatRoom;

    @Autowired
    private ChatRoomV2 chatRoomV2;

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

    @PostMapping("/create")
    public void create(@RequestParam String admin, @RequestParam String roomName) {
        chatRoomV2.createChatRoom(admin, roomName);
        core.notifyEveryone(roomName + " is created");
    }

    @PostMapping("/sendv2")
    public void sendv2(@RequestParam String username, @RequestParam String roomName, @RequestParam String message) {
        chatRoomV2.send(username, roomName, message);
    }

    @GetMapping(value = "/receive", produces = "text/event-stream")
    public Flux<String> receive(@RequestParam String username, @RequestParam String roomName) {
        return chatRoomV2.receive(username, roomName);
    }

    @GetMapping(value = "/get_message_from_first", produces = "text/event-stream")
    public Flux<String> getMessageFromFirst(@RequestParam String username, @RequestParam String roomName) {
        return chatRoomV2.getMessageFromFirst(username, roomName);
    }

    @GetMapping(value = "/get_message_from_last", produces = "text/event-stream")
    public Flux<String> getMessageFromLast(@RequestParam String username, @RequestParam String roomName) {
        return chatRoomV2.getMessageFromLast(username, roomName);
    }

    @GetMapping(value = "/get_message_from_timestamp", produces = "text/event-stream")
    public Flux<String> getMessageFromTimestamp(@RequestParam String username, @RequestParam String roomName, @RequestParam Date time) {
        return chatRoomV2.getMessageFromTimestamp(username, roomName, time);
    }

    @GetMapping(value = "/get_message_from_offset", produces = "text/event-stream")
    public Flux<String> getMessageFromOffset(@RequestParam String username, @RequestParam String roomName, @RequestParam long offset) {
        return chatRoomV2.getMessageFromOffset(username, roomName, offset);
    }

}
