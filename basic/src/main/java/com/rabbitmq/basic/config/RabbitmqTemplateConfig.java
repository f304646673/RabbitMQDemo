package com.rabbitmq.basic.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqTemplateConfig {

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        return new org.springframework.amqp.rabbit.core.RabbitTemplate(connectionFactory);
    }

    @Bean(name = "rabbitTemplateWithMandatory")
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplateWithMandatory(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        return createRabbitTemplate(connectionFactory, true);
    }

    @Bean(name = "rabbitTemplateWithoutMandatory")
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplateWithoutMandatory(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        return createRabbitTemplate(connectionFactory, false);
    }

    private org.springframework.amqp.rabbit.core.RabbitTemplate createRabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, boolean mandatory) {
        org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate = null;
        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.setPublisherReturns(true);
            rabbitTemplate = new org.springframework.amqp.rabbit.core.RabbitTemplate(cachingConnectionFactory);
        } else {
            throw new IllegalArgumentException("The provided connectionFactory is not an instance of CachingConnectionFactory");
        }

        rabbitTemplate.setMandatory(mandatory);
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println(STR."Message returned: \{returned.getReplyText()} \n message: \{new String(returned.getMessage().getBody())} \n correlation id: \{returned.getMessage().getMessageProperties().getCorrelationId()} \n routing key: \{returned.getRoutingKey()} \n exchange: \{returned.getExchange()} \n reply code: \{returned.getReplyCode()}"
            );
        });
        return rabbitTemplate;
    }

    private org.springframework.amqp.rabbit.core.RabbitTemplate createRabbitTemplateWithAck(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, boolean mandatory) {
        org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate = null;
        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            rabbitTemplate = new org.springframework.amqp.rabbit.core.RabbitTemplate(cachingConnectionFactory);
        } else {
            throw new IllegalArgumentException("The provided connectionFactory is not an instance of CachingConnectionFactory");
        }

        rabbitTemplate.setMandatory(mandatory);
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println(STR."Message returned: \{returned.getReplyText()} \n message: \{new String(returned.getMessage().getBody())} \n correlation id: \{returned.getMessage().getMessageProperties().getCorrelationId()} \n routing key: \{returned.getRoutingKey()} \n exchange: \{returned.getExchange()} \n reply code: \{returned.getReplyCode()}"
            );
        });
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (null == correlationData) {
                return;
            }
            if (ack) {
                System.out.println(STR."Message confirmed: \{correlationData.getId()}");
            } else {
                System.out.println(STR."Message not confirmed: \{correlationData.getId()} with cause: \{cause}");
            }
        });
        return rabbitTemplate;
    }

}
