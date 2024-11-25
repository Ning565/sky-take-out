package com.sky.controller.user;


import com.sky.config.RedisConfiguration;
import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

// 同样的类在user和admin均出现，bean重复，为restcontrller起个别名
@RestController("userShopController")
@Slf4j
@RequestMapping("/user/shop")
@Api(tags = "商店相关接口")
public class ShopController {
    @Autowired
    private RedisTemplate redisTemplate;
    // 常量表示
    public static final String KEY = "SHOP_STATUS";

    @GetMapping("/status")
    @ApiOperation("获取商店营业状态")
    public Result<Integer> getStatus(){
        ValueOperations springValueOperations = redisTemplate.opsForValue();
        Integer status = (Integer) springValueOperations.get(KEY);
        log.info("获取到店铺的营业状态为：{}",status == 1 ? "营业中" : "已打烊");
        return Result.success(status);
    }
}
