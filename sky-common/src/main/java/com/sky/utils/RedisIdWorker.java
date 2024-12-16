package com.sky.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳，基准时间戳2022.1.1
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    /**
     * 全局ID生成器：0 + 时间戳31(当前时间的偏移) + 序列号 32(秒内的计数器，支持每秒产生2^32个不同ID)
     */
    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPre) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowTimeStamp = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowTimeStamp - BEGIN_TIMESTAMP;
        // 2.生成序列号
        // 2.1.获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPre + ":" + date);
        // 3.拼接返回
        return timeStamp << COUNT_BITS | count;
    }

}
