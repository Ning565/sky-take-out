package com.sky.utils;

import cn.hutool.core.lang.func.Func;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;


import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

@Component
public class CacheClient {
    public static final Long CACHE_NULL_TTL = 2L;
    public static final String LOCK_VOUCHER_KEY = "lock:voucher:";
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 实现方法：
     * 1. 按照过期时间设置Redis存储值
     * 2. 按照逻辑过期时间设置存储值
     * 3. 实现缓存空值——缓存穿透
     * 4. 实现加锁 + 解锁 利用redis的setnx设置key-value
     * 5. 实现互斥锁——缓存击穿（效率低，一致性好）
     * 6. 实现逻辑过期互斥锁，开启新线程——缓存击穿（并行效率高，最终方案）
     */
    // 创建十个线程的线程池，用于最终方案的异步缓存
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 1.按照过期时间存储redis值
     *
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        // 字符串序列化为String类型，存入数据库
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    /**
     * 2.按照逻辑过期时间存储redis值，设置逻辑过期的值，存储逻辑过期时间时，采用组合的思想（继承），创建一个新类增加逻辑过期时间
     *
     * @param key
     * @param value
     * @param time
     * @param timeUnit
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit) {
        // 设置逻辑过期,当前时间增加设置的time，统一转化为秒单位
        RedisData redisData = RedisData.builder().data(value).expireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time))).build();
        // 存入redis中
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 3.设置空值防止缓存穿透，传入key的前缀，ID名称（按照ID查询），返回值的类型class类（封装结果），调用数据库查询的函数，time
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dataBaseCall
     * @param time
     * @param timeUnit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithPassThrouth(String keyPrefix, ID id, Class<R> type, Function<ID, R> dataBaseCall, Long time, TimeUnit timeUnit) {
        // 获取真正的key值
        String key = keyPrefix + "::" + id;
        // 1.按照key查询
        String result = (String) redisTemplate.opsForValue().get(key);
        // 2.查到的不是null而且不是空值（有值）
        if (StrUtil.isNotBlank(result)) {
            // 2.1 返回Json格式化后的值
            return JSONUtil.toBean(result, type);
        }
        // 2.2 查到的非null（有值），但为空值（""）时，直接返回null
        if (result != null) {
            return null;
        }
        // 3.不存在，根据id查询数据库
        R r = dataBaseCall.apply(id);
        // 4.查询结果如果不存在，则写一个空值，后续不需要进入到数据库了
        // 设置一个默认过期时间，防止后续真数据来了
        if (r == null) {
            redisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回null
            return null;
        }
        // 5.如果查询存在，则写入到redis
        // 调用上面的写入数据的set函数
        this.set(key, r, time, timeUnit);
        // R已经是R类型的返回对象，无需转换
        return r;
    }

    /**
     * 4.上锁，利用商品/优惠券等的ID作为key（lock:shop:1），热点key问题查询，并设置10分钟时间
     * 解锁，直接删除这个key
     *
     * @param key
     * @return
     */
    public boolean tryLock(String key) {
        Boolean lockFlag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        // 将 flag 的值转换为 boolean 类型的 true 或 false，代码一致性
        return BooleanUtil.isTrue(lockFlag);
    }

    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 5.利用互斥锁，防止缓存击穿问题，利用setnx设置key为热点信息，获取锁失败时利用循环来等待，超过等待次数则返回null
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dataBasecall
     * @param time
     * @param timeUnit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dataBasecall, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = (String) redisTemplate.opsForValue().get(key);
        // 2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        String lockKey = LOCK_VOUCHER_KEY + id;
        R r = null;
        int retryCount = 0;
        final int maxRetries = 3;  // 最大重试次数
        while (retryCount < maxRetries) {
            try {
                // 4.1 尝试获取锁
                boolean isLock = tryLock(lockKey);
                // 4.2 如果获取锁成功，执行数据库查询
                if (isLock) {
                    try {
                        r = dataBasecall.apply(id);
                        // 5.如果数据库返回 null，缓存空值并返回
                        if (r == null) {
                            this.set(key, "", CACHE_NULL_TTL, TimeUnit.DAYS);
                            return null;
                        }
                        // 6.如果数据库返回数据，将数据存入缓存
                        this.set(key, r, time, timeUnit);
                        break; // 成功获取数据，退出循环
                    } finally {
                        // 7.释放锁
                        unlock(lockKey);
                    }
                }
            } catch (Exception e) {
                // 错误处理（可根据需要选择处理方式）
                throw new RuntimeException(e);
            }

            // 4.3 如果获取锁失败，休眠并重试
            retryCount++;
            try {
                Thread.sleep(50);  // 等待一段时间再重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // 恢复中断状态
                throw new RuntimeException(e);
            }
        }

        // 如果锁一直获取失败，返回 null
        if (r == null) {
            return null;
        }
        return r;
    }

    /**
     * 实现逻辑过期互斥锁，开启新线程——缓存击穿（并行效率高，最终方案）
     * 第一次查询的都会是返回null，需要把数据提前缓存进redis ：获取锁的主线程等待一下，查到结果再返回
     * TODO 小缺陷 大家会同时抢夺锁，返回null
     * 如果是主线程，在分线程完成查询任务后，重新构建数据并返回，解决第一次返回为null的问题
     *
     * @param keyPrefix
     * @param id
     * @param type
     * @param dataBaseCall
     * @param time
     * @param timeUnit
     * @param <R>
     * @param <ID>
     * @return
     */
    public <R, ID> R queryById(String keyPrefix, ID id, Class<R> type, Function<ID, R> dataBaseCall, Long time, TimeUnit timeUnit) throws InterruptedException {
        String key = keyPrefix + id;
        // 1. 查询redis数据库
        String result = (String) redisTemplate.opsForValue().get(key);
        R r = null;
        RedisData redisData = null;
        // 2.1 如果既不为空，也不为null，判断过期与否再返回
        if (StrUtil.isNotBlank(result)) {
            // 2.2 如果有值且不为空，先把json反序列化为对象，判断过期时间，未过期，直接返回店铺信息
            redisData = JSONUtil.toBean(result, RedisData.class);
            // 反序列化 redisData.getData() 返回的是一个 JSON 对象,将JSON 数据，转换为指定的 Java 对象类型 type
            // 返回的r是旧数据
            r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            // 判断旧数据的时间，逻辑过期时间比现在早，即还没过期
            LocalDateTime expireTime = redisData.getExpireTime();
            if (LocalDateTime.now().isBefore(expireTime)) {
                return r;
            }
        }
        // 如果不为null，则为空
        if (result == "") return null;
        // 过期了则重建
        // 3.缓存重建
        String lockKey = LOCK_VOUCHER_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 4.获取互斥锁
        // 4.1 获取成功开启一个新线程重建数据库，设置过期时间为逻辑过期时间，最后返回数据
        if (isLock) {
            // 成功，开启独立线程，实现缓存重建
            // 使用了一个线程池 CACHE_REBUILD_EXECUTOR 提交一个异步任务，submit() 方法会将任务提交到线程池中
            // () -> { ... } Lambda 表达式，它定义了一个匿名函数，也可以理解为一个线程要执行的任务
            CACHE_REBUILD_EXECUTOR.submit(
                    () -> {
                        try {
                            R rNew = dataBaseCall.apply(id);
                            if (rNew != null) {
                                this.setWithLogicalExpire(key, rNew, time, timeUnit);
                            } else this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);


                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            unlock(lockKey);
                        }
                    }
            );
            // 如果是主线程，在分线程完成查询任务后，重新构建数据并返回，解决第一次返回为null的问题
            // 最多等待0.5秒钟返回
            Thread.sleep(500);
            result = (String) redisTemplate.opsForValue().get(key);
            redisData = JSONUtil.toBean(result, RedisData.class);
            r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            return r;
        }
        // 获取失败的直接返回旧的信息
        return r;
    }
}
