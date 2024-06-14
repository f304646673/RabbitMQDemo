package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.SetterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Setter", description = "The Setter API")
@RestController
@RequestMapping("/setter")
public class SetterController {
    @Autowired
    private SetterService setterService;

    @Operation(summary = "Send a message", description = "Send a message to SetterService")
    @PostMapping("/send")
    public void send(@RequestParam String message) {
        setterService.sendMessage(message);
    }
    
}
