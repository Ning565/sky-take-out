package com.sky.service.impl;

import com.sky.controller.admin.DishService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品，同时增加批量口味
     *
     * @param dishDTO
     */
    @Override
    public void save(DishDTO dishDTO) {
        // 1.增加菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        // 2.批量增加菜品口味
        // 主键回显得到id
        Long dishId = dish.getId();
        // 批量增加口味
        List<DishFlavor> dishFlavors = dishDTO.getFlavors();
        if (!CollectionUtils.isEmpty(dishFlavors)) {
            // 遍历List中每个元素，批量获取dishID
            dishFlavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(dishFlavors);
        }
    }
}
