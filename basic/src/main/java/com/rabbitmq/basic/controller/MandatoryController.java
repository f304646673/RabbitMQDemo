package com.rabbitmq.basic.controller;

import com.rabbitmq.basic.service.MandatoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Mandatory", description = "The Mandatory API")
@RestController
@RequestMapping("/mandatory")
public class MandatoryController {
    @Autowired
    private MandatoryService mandatoryService;

    @Operation(summary = "Send a message", description = "Send a message to MandatoryService")
    @Parameters({
        @Parameter(name = "exchangeName", description = "The exchange name", required = true),
        @Parameter(name = "routingKey", description = "The routing key", required = true),
        @Parameter(name = "message", description = "The message", required = true)
    })
    @PostMapping("/send")
    public void send(@RequestParam String exchangeName, @RequestParam String routingKey, @RequestParam String message) {
        mandatoryService.send(exchangeName, routingKey, message);
    }
}
