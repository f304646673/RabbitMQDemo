package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.ConsumerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    @Autowired
    private ConsumerService consumerService;

    @Operation(summary = "Start listening", description = "Start listening to a queue")
    @Parameters({
        @Parameter(name = "queueName", description = "The queue name", required = true)
    })
    @PutMapping("/startListening")
    public void startListening(@RequestParam String queueName) {
        consumerService.startListening(queueName);
    }

    @Operation(summary = "Stop listening", description = "Stop listening to a queue")
    @Parameters({
        @Parameter(name = "queueName", description = "The queue name", required = true)
    })
    @PutMapping("/stopListening")
    public void stopListening(@RequestParam String queueName) {
        consumerService.stopListening(queueName);
    }
}
