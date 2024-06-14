package com.rabbitmq.basic.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OperationService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public boolean addSimpleQueue(String queueName) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueDeclare(queueName, false, false, false, null);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteSimpleQueue(String queueName) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueDelete(queueName);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addDirectExchange(String exchangeName) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.exchangeDeclare(exchangeName, "direct", false);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteDirectExchange(String exchangeName) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.exchangeDelete(exchangeName);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean bindQueueToExchange(String queueName, String exchangeName, String routingKey) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueBind(queueName, exchangeName, routingKey);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean unbindQueueFromExchange(String queueName, String exchangeName, String routingKey) {
        try {
            rabbitTemplate.execute(channel -> {
                channel.queueUnbind(queueName, exchangeName, routingKey);
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getAllQueues() throws JsonProcessingException {
        final String rabbitmqManagementApiUrl = "http://" + rabbitTemplate.getConnectionFactory().getHost() + ":15672/api/queues";
        final String username = rabbitTemplate.getConnectionFactory().getUsername();
        final String password = "fangliang";

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));

        String response = restTemplate.getForObject(rabbitmqManagementApiUrl, String.class);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        List<String> queueNames = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode node : root) {
                String queueName = node.get("name").asText();
                queueNames.add(queueName);
            }
        }

        return queueNames;
    }

    public boolean bindAllQueueToXConsistentHashExchange(String exchangeName) throws JsonProcessingException {
        List<String> queueNames = getAllQueues();
        for (String queueName : queueNames) {
            if (!bindQueueToExchange(queueName, exchangeName, "1")) {
                return false;
            }
        }
        return true;
    }

    public boolean sendToXConsistentHashExchange(String exchangeName, Long count) {
        for (int i = 0; i < count; i++) {
            int hash = Integer.hashCode(i);
            rabbitTemplate.convertAndSend(exchangeName, String.valueOf(hash), "Message " + i);
        }
        return true;
    }

    public boolean sendToXConsistentHashExchangeWithHeader(String exchangeName, Long count) {
        for (int i = 0; i < count; i++) {
            int hash = Integer.hashCode(i);
            Message message = MessageBuilder.withBody(("Message " + i).getBytes())
                .setHeader("hash", hash)
                .build();
            rabbitTemplate.convertAndSend(exchangeName, "",  message);
        }
        return true;
    }

    public boolean sendToXConsistentHashExchangeWithMessageId(String exchangeName, Long count) {
        for (int i = 0; i < count; i++) {
            int hash = Integer.hashCode(i);
            Message message = MessageBuilder.withBody(("Message " + i).getBytes())
                .setMessageId(String.valueOf(hash))
                .build();
            rabbitTemplate.convertAndSend(exchangeName, "",  message);
        }
        return true;
    }
    
}
