package com.rabbitmq.consumer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.consumer.service.ConsumerService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {
    
    @Autowired
    private ConsumerService comsumerService;

   
    @GetMapping(value = "/listen", produces = "text/event-stream")
    @ResponseBody
    public Flux<String> listen(@RequestParam String queueName) {
        // Return the current time every second
        // return Flux.interval(java.time.Duration.ofSeconds(1))
        //             .map(tick -> java.time.LocalDateTime.now().toString()).take(10);
        return comsumerService.listen(queueName);
    }
}
