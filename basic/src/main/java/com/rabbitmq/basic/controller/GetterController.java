package com.rabbitmq.basic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.basic.service.GetterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Getter", description = "The Getter API")
@RestController
@RequestMapping("/getter")
public class GetterController {
    @Autowired
    private GetterService getterService;

    @Operation(summary = "Get a message", description = "Get a message from GetterService")
    @GetMapping("/get")
    public String get() {
        return getterService.getMessage();
    }
    
}
