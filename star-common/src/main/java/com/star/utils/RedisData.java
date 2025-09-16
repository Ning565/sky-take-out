package com.star.utils;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;

    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }

    public static RedisDataBuilder builder() {
        return new RedisDataBuilder();
    }
    public static class RedisDataBuilder {
        private LocalDateTime expireTime;
        private Object data;
        public RedisDataBuilder expireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
            return this;
        }
        public RedisDataBuilder data(Object data) {
            this.data = data;
            return this;
        }
        public RedisData build() {
            RedisData redisData = new RedisData();
            redisData.setExpireTime(this.expireTime);
            redisData.setData(this.data);
            return redisData;
        }
    }
}
