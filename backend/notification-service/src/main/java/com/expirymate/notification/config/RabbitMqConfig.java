package com.expirymate.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
	public static final String EXCHANGE = "expirymate.events";

    public static final String WELCOME_QUEUE = "notification.welcome.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    public static final String EXPIRY_QUEUE =  "notification.expiry.queue";
    public static final String EXPIRY_ROUTING_KEY = "document.expiring";
    
    @Bean
    public TopicExchange expiryMateExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue welcomeQueue() {
        return new Queue(WELCOME_QUEUE, true);
    }

    @Bean
    public Binding welcomeBinding(
            Queue welcomeQueue,
            TopicExchange expiryMateExchange) {

        return BindingBuilder
                .bind(welcomeQueue)
                .to(expiryMateExchange)
                .with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Queue expiryQueue() {
        return new Queue(EXPIRY_QUEUE, true);
    }

    @Bean
    public Binding expiryBinding(
            Queue expiryQueue,
            TopicExchange expiryMateExchange) {

        return BindingBuilder
                .bind(expiryQueue)
                .to(expiryMateExchange)
                .with(EXPIRY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
