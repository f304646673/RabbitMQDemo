package com.rabbitmq.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.chat.service.Core;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private Core core;

    @PostMapping(value = "/login", produces = "text/event-stream")
    public Flux<String> login(@RequestParam String username) {
        return core.Login(username);
    }
    
}
