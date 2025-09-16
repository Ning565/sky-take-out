package com.star.mapper;

import com.star.entity.DishTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DishTagMapper {
    // 新增标签
    int insert(DishTag tag);

    // 根据ID查询
    DishTag selectById(@Param("id") Long id);

    // 根据dishId查询所有标签
    List<DishTag> selectByDishId(@Param("dishId") Long dishId);

    // 根据标签类型和菜品ID查询
    List<DishTag> selectByDishIdAndType(@Param("dishId") Long dishId, @Param("tagType") String tagType);

    // 更新标签
    int update(DishTag tag);

    // 删除标签
    int deleteById(@Param("id") Long id);
} 