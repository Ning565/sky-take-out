package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.VoucherOrder;

import com.sky.mapper.VoucherOrderMapper;
import com.sky.result.Result;
import com.sky.service.IVoucherOrderService;
import com.sky.service.IVoucherSeckillService;
import com.sky.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    RedissonClient redissonClient;
    @Autowired
    private IVoucherSeckillService seckillVoucherService;
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

    // 创建一个线程池，包含单个线程，去异步处理用户下单
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct //当前类初始化完毕执行
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler()); // 提交任务
    }

    private class VoucherOrderHandler implements Runnable {
        // 新建的任务：开启以后就会不断获取队列中的信息
        @Override
        public void run() {
            // 不断取信息
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    // Redis Stream数据结构: ID field1/2/3 value1/2/3，对应MapRecord，而Redis Stream 有多条信息，因而读出来一个列表
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (CollectionUtils.isEmpty(list)) {
                        // 如果是空的，继续等待取
                        continue;
                    }
                    // 3.解析数据,解析订单中的信息
                    // Redis Stream 中的消息数据是以 键值对 的形式存储的，业务逻辑需要 Java 对象，将 Map 数据转换为 Java 对象后
                    // 生成的任意序列号格式：
                    //XRANGE stream.orders - +
                    //1) "1681234567890-0"
                    //   1) "userId"
                    //   2) "123"
                    //   3) "voucherId"
                    //   4) "1001"
                    //   5) "id"
                    //   6) "456789"
                    MapRecord<String, Object, Object> record = list.get(0);
                    // 获取三个属性
                    Map<Object, Object> value = record.getValue(); // record是key（用户id） -filed -value（订单id,对应order的id）
                    // 快速将 Map 转换成 Java Bean VoucherOrder类的对象，避免手动逐一赋值
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 4.创建订单
                    createVoucherOrder(voucherOrder);
                    // 5.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create("stream.orders", ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    createVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }

    private void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order" + userId);
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();
        if (!isLock) {
            log.info("不允许重复下单");
            return ;
        }
        // 获取锁成功
        try{
            // 查询订单，看看本次的ID是否存在这个订单
            long count = query().eq("user_id",userId).eq("voucher_id",voucherId).count();
            // 如果该用户买过
            if(count >  0){
                log.info("不允许重复下单");
                return ;
            }
            // 扣减库存判断超卖
            boolean success = seckillVoucherService.update().setSql("stock = stock - 1") // set stock = stock - 1
                    .eq("voucher_id", voucherId).gt("stock", 0).update();// where voucher_id = ? and stock > 0
            if (!success) {
                log.info("库存不足");
                return ;
            }
            // 7.创建订单
            save(voucherOrder);
        }finally {
            //最后释放锁
            redisLock.unlock();
        }
    }

    @Override
    public Result purchase(Long voucherId) {
        Long userId = 1010108L;
        long orderId = redisIdWorker.nextId("order");
        // 1.执行Lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId));
        // 2.按照执行结果返回给用户
        int r = result.intValue();
        if (r == 1) {
            return Result.error("库存不足");
        } else if (r == 2) {
            return Result.error("同一用户不能重复下单");
        }
        return Result.success("下单成功，订单号：" + orderId);
    }
}
