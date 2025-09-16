package com.star.mapper;

import com.star.entity.SetmealDish;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    // select setmeal_id from setmeal_dish where dish_id in (???)
    List<Long> getByDishIds(List<Long> dishIds);


    void insertBatch(List<SetmealDish> setmealDishes);

    @Select("select * from star_food_chain.setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getById(Long id);

    void deleteByIds(List<Long> setmealIds);
}
