package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.NonmandatoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Nonmandatory", description = "The Nonmandatory API")
@RestController
@RequestMapping("/nonmandatory")
public class NonmandatoryController {
    @Autowired
    private NonmandatoryService nonmandatoryService;

    @Operation(summary = "Send a message", description = "Send a message to NonmandatoryService")
    @Parameters({
        @Parameter(name = "exchangeName", description = "The exchange name", required = true),
        @Parameter(name = "routingKey", description = "The routing key", required = true),
        @Parameter(name = "message", description = "The message", required = true)
    })
    @PostMapping("/send")
    public void send(@RequestParam String exchangeName, @RequestParam String routingKey, @RequestParam String message) {
        nonmandatoryService.send(exchangeName, routingKey, message);
    }
}
