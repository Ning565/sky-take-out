package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    // select setmeal_id from setmeal_dish where dish_id in (???)
    List<Long> getByDishIds(List<Long> dishIds);
}
