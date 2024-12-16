package com.sky.service.impl;

import com.rabbitmq.client.Channel;
import com.sky.config.RabbitMqConfig;
import com.sky.entity.VoucherOrder;
import com.sky.service.IVoucherSeckillService;
import com.sky.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.Message;


@Service
@Slf4j
public class VoucherOrderConsumer {

    @Autowired
    private IVoucherOrderService voucherOrderService;

    @Autowired
    private IVoucherSeckillService seckillVoucherService;
    @Autowired
    RedissonClient redissonClient;

    // 使用@RabbitListener注解来监听消息队列
    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void processOrder(VoucherOrder voucherOrder, Channel channel, Message message){
        // 处理订单逻辑，创建订单并扣减库存
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 消息处理开始，先处理订单逻辑
        try {
            // 创建锁对象
            RLock redisLock = redissonClient.getLock("lock:order" + userId);
            boolean isLock = redisLock.tryLock();
            // 锁获取失败直接确认，不需要重新入队
            if (!isLock) {
                log.info("不允许重复下单");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }
            try {
                // 查询订单是否存在
                long count = voucherOrderService.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
                if (count > 0) {
                    log.info("不允许重复下单");
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }
                // 扣减库存
                boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                        .eq("voucher_id", voucherId).gt("stock", 0).update();
                if (!success) {
                    log.info("库存不足");
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }
                // 创建订单
                voucherOrderService.save(voucherOrder);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

            } finally {
                redisLock.unlock();
            }
        } catch (Exception e) {
            log.error("处理订单失败", e);
            // 异常时，发送 nack，重新入队重试
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (Exception ex) {
                log.error("确认消息失败", ex);
            }
        }
    }
}
