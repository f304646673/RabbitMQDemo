package com.rabbitmq.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class UserService {
    @Autowired
    private Core core;

    public Flux<String> Login(String username) {
        return core.Login(username);
    }

}