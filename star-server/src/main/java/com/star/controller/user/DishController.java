package com.star.controller.user;

import com.star.constant.CacheConstant;
import com.star.constant.StatusConstant;
import com.star.entity.Dish;
import com.star.result.Result;
import com.star.service.DishService;
import com.star.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.star.constant.CacheConstant.CACHE_DISH_TTL;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     * Redis来缓存菜品数据，减少数据库查询操作
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(@RequestParam Long categoryId) {
        // 1.构造Redis的key值
        String key = "cache:dish:" + categoryId;
        // 2.查询当前key是否在redis存储，如果存储则return
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (!CollectionUtils.isEmpty(list)) return Result.success(list);
        // 3.未存储，查询结果以后则将其存储到redis
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE); //查询起售中的菜品

        list = dishService.listWithFlavor(dish);
        // 将查询结果存储，设置有效期
        redisTemplate.opsForValue().set(key,list,CACHE_DISH_TTL, TimeUnit.MINUTES);
        return Result.success(list);
    }

}
