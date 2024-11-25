package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){ // RedisConnectionFactory 是一个接口，用于创建 Redis 的连接
        log.info("开始创建Redis模版对象...");
        // RedisTemplate Spring Data Redis提供的一个用于操作 Redis 数据库的核心类，它封装了对 Redis 的常见操作
        RedisTemplate redisTemplate = new RedisTemplate();
        // 设置Redis连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置Redis key序列化器  new字符串类型的序列化器
        // 不进行设置，默认会使用 Java 的序列化方式来处理对象，会生成较大的二进制数据，设置以后数据库的key都会处理成String
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
