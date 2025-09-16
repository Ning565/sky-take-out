package com.star.service;

import com.star.dto.SetmealDTO;
import com.star.dto.SetmealPageQueryDTO;
import com.star.entity.Setmeal;
import com.star.result.PageResult;
import com.star.vo.DishItemVO;
import com.star.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    void save(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO getById(Long id);

    void deleteByIds(List<Long> ids);

    void update(SetmealDTO setmealDTO);

    void startOrStop(Long id, Integer status);

    List<Setmeal> list(Setmeal setmeal);

    List<DishItemVO> getDishItemById(Long id);
}
