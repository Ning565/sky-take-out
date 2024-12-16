package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMqConfig {

    // 定义交换机
    public static final String EXCHANGE = "voucher_exchange";
    // 定义队列
    public static final String QUEUE = "voucher_order_queue";
    // 定义路由
    public static final String ROUTING_KEY = "voucher.order";

    @Bean
    public Exchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue queue() {
        // 队列是持久化的，即当RabbitMQ 服务器重启时，队列及其中的消息不会丢失，队列依然存在
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_KEY).noargs();
    }
}
