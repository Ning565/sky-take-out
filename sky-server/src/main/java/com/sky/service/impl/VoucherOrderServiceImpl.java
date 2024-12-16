package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.config.RabbitMqConfig;
import com.sky.entity.VoucherOrder;

import com.sky.mapper.VoucherOrderMapper;
import com.sky.result.Result;
import com.sky.service.IVoucherOrderService;
import com.sky.service.IVoucherSeckillService;
import com.sky.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    /**
     * 处理用户请求，执行 Redis Lua 脚本库存扣减与创建订单扔到队列，直接返回请求
     *
     * @param id
     */
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedisIdWorker redisIdWorker;

    @Autowired
    private IVoucherSeckillService seckillVoucherService;
    // rabbitMq队列
    @Autowired
    private AmqpTemplate amqpTemplate;
    // 定义若干静态常量类，首次运行便加载
    // 用于封装 Redis 的 Lua 脚本,脚本执行返回Long类型的数据
    // static类级别的变量，类加载而被初始化；final：常量只能被赋值一次
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    // 静态代码块用于对静态变量进行初始化。
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));// ClassPathResource：Spring下的资源类，脚本文件所在位置，加载类路径（classpath）下的文件
        SECKILL_SCRIPT.setResultType(Long.class);// 返回类型
    }



    @Override
    public Result purchase(Long voucherId) {
        Long userId = 1010108L;
        long orderId = redisIdWorker.nextId("order");
        // 1.执行Lua脚本，最后不把结果推送到redis stream中了
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId));
        // 2.按照执行结果返回给用户
        int r = result.intValue();
        if (r == 1) {
            return Result.error("库存不足");
        } else if (r == 2) {
            return Result.error("同一用户不能重复下单");
        }
        // 3.将消息发送到RabbitMQ队列中
        // 3.1 创建订单对象
        // 创建一个订单对象
        VoucherOrder voucherOrder = VoucherOrder.builder().userId(userId).voucherId(voucherId).id(orderId).build();
        // 3.2 发送消息
        amqpTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, voucherOrder);
        return Result.success("下单成功，订单号：" + orderId);
    }
}
