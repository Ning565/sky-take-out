package com.star.service;

import com.star.dto.DishDTO;
import com.star.dto.DishPageQueryDTO;
import com.star.entity.Dish;
import com.star.result.PageResult;
import com.star.vo.DishVO;

import java.util.List;

public interface DishService {
    void save(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTODTO);

    void deletByIds(List<Long> ids);

    DishVO getById(Long id);

    void update(DishDTO dishDTO);

    void startOrStop(Integer status, Long id);

    List<Dish> getByCateId(Long cateId);

    List<DishVO> listWithFlavor(Dish dish);
}
