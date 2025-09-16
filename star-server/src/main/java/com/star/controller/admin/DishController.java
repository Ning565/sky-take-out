package com.star.controller.admin;


import com.star.dto.DishDTO;
import com.star.dto.DishPageQueryDTO;
import com.star.entity.Dish;
import com.star.result.PageResult;
import com.star.result.Result;
import com.star.service.DishService;
import com.star.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品相关接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     *
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.save(dishDTO);
        // 清理缓存
        String key = "dish" + dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询回显：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result deletByIds(@RequestParam List<Long> ids) {
        log.info("批量删除菜品,ids={}", ids);
        dishService.deletByIds(ids);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 查询回显
     *
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("按照ID查询回显")
    public Result<DishVO> getById(@PathVariable Long id) {
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品信息
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        dishService.update(dishDTO);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 设置菜品起售状态
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("编辑菜品起售状态")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("编辑菜品起售状态: staus = {}", status);
        dishService.startOrStop(status, id);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类ID查询菜品，一个分类下很多菜品，返回List
     *
     * @param categoryId
     * @return
     * @GetMapping时，Spring 默认会根据请求的查询参数名称来匹配控制器方法中的参数吗，参数与前端一致不需要@RequestParam，否则需要
     */
    @GetMapping("/list")
    @ApiOperation("根据分类ID查询菜品")
    public Result<List<Dish>> getByCateId(Long categoryId) {
        log.info("根据分类ID查询菜品，categoryId={}", categoryId);
        List<Dish> dishes = dishService.getByCateId(categoryId);
        return Result.success(dishes);
    }

    /**
     * 清理redis缓存，涉及到增删改的时候数据库改变，缓存也需要变
     * @param pattern
     */
    private void cleanCache(String pattern) {
        // redis不能处理dish_*，用keys获取：所有与 pattern 匹配的键的
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
