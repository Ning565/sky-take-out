package com.star.controller.admin;


import com.star.config.RedisConfiguration;
import com.star.constant.CacheConstant;
import com.star.constant.StatusConstant;
import com.star.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

// 同样的类在user和admin均出现，bean重复，为restcontrller起个别名
@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "商店相关接口")
public class ShopController {
        @Autowired
        private RedisTemplate redisTemplate;
        // 常量表示


        @PutMapping("/{status}")
        @ApiOperation("设置商店营业状态")
        public Result setStatus(@PathVariable Integer status){
            log.info("设置商店营业状态:{}",status == 1 ? "营业中" : "已打烊");
            // 利用Redis设置
            ValueOperations springValueOperations = redisTemplate.opsForValue();
            springValueOperations.set(CacheConstant.SHOP_STATUS, status);
            return Result.success();
        }
        @GetMapping("/status")
        @ApiOperation("获取商店营业状态")
        public Result<Integer> getStatus(){
            // 为实例变量，不能在类中书写（成员变量，没有初始化），必须在方法中
            ValueOperations springValueOperations = redisTemplate.opsForValue();
            Integer status = (Integer) springValueOperations.get(CacheConstant.SHOP_STATUS);
            log.info("获取到店铺的营业状态为：{}",status == 1 ? "营业中" : "已打烊");
            return Result.success(status);
        }
}
