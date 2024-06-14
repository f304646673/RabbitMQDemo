package com.rabbitmq.basic.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.basic.service.OperationService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Operation", description = "The Operation API")
@RestController
@RequestMapping("/operation")
public class OperationController {
    @Autowired
    private OperationService operationService;

    @GetMapping("queue/get")
    public List<String> getAllQueues() throws JsonProcessingException {
        return operationService.getAllQueues();
    }

    @GetMapping("send/message")
    public void sendToXConsistentHashExchange(
        @RequestParam String exchangeName,
        @RequestParam Long count)
    {
        operationService.sendToXConsistentHashExchange(exchangeName, count);
    }
}
