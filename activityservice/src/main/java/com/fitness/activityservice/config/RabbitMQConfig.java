package com.fitness.activityservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.template.exchange}")
    private String exchange;

    @Value("${spring.rabbitmq.template.default-receive-queue}")
    private String queue;

    @Value("${spring.rabbitmq.template.routing-key}")
    private String routingKey;

    @Bean
    public Queue activityQueue(){
        return new Queue(queue, true);
    }

    @Bean
    public DirectExchange activityExchange(){
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, DirectExchange activityExchange){
        return BindingBuilder.bind(activityQueue).to(activityExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
