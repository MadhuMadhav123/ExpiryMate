package com.expirymate.auth.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String EXCHANGE = "expirymate.events";

	public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

	@Bean
	public TopicExchange expiryMateExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	public Jackson2JsonMessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
