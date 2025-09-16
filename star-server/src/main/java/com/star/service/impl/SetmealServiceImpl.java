package com.star.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.star.annotation.AutoFill;
import com.star.constant.MessageConstant;
import com.star.constant.StatusConstant;
import com.star.dto.SetmealDTO;
import com.star.dto.SetmealPageQueryDTO;
import com.star.entity.Dish;
import com.star.entity.Setmeal;
import com.star.entity.SetmealDish;
import com.star.enumeration.OperationType;
import com.star.exception.DeletionNotAllowedException;
import com.star.exception.SetmealEnableFailedException;
import com.star.mapper.DishMapper;
import com.star.mapper.SetmealDishMapper;
import com.star.mapper.SetmealMapper;
import com.star.result.PageResult;
import com.star.service.SetmealService;
import com.star.vo.DishItemVO;
import com.star.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    SetmealMapper setmealMapper;
    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    DishMapper dishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 1.增加套餐内容到套餐表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);
        // 2.批量增加菜品到套餐
        // 先获取当前套餐的ID和添加的菜品数量
        Long setmealId = setmeal.getId();
        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();
        // 套餐肯定有菜品（必选），所以没必要判断是否为空
        dishList.forEach(setmealDish -> {setmealDish.setSetmealId(setmealId);});
        setmealDishMapper.insertBatch(dishList);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealPageQueryDTO,setmeal);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmeal);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 按照套餐ID查询套餐，同时还需要查询套餐的菜品表
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        // 按照套餐ID查询套餐信息
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        // 按照套餐ID查询菜品信息
        List<SetmealDish> setmealDish = setmealDishMapper.getById(id);
        setmealVO.setSetmealDishes(setmealDish);
        return setmealVO;
    }

    /**
     * 按照套餐ID批量删除，删除需要条件
     * @param ids
     */
    @Override
    public void deleteByIds(List<Long> ids) {
        // 判断无法被删除的：起售中的
        for (Long id : ids) {
            Setmeal setmealTemp = setmealMapper.getById(id);
            if (setmealTemp.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 先批量删除套餐表
        setmealMapper.deleteByIds(ids);
        // 再批量删除套餐-菜品对应表
        setmealDishMapper.deleteByIds(ids);
    }

    /**
     * 修改套餐，对回显以后的内容进行修改，包含修改套餐信息，修改套餐关联的菜品信息
     * @param setmealDTO
     */
    @Override
    @AutoFill(OperationType.UPDATE)
    public void update(SetmealDTO setmealDTO) {
        // 修改套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //修改套餐关联菜品信息 : 先删除，再插入
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 把当前的套餐ID创建一个List存储（其实只有一个值）
        List<Long> idList = new ArrayList<>();
        idList.add(setmealDTO.getId());
        setmealDishMapper.deleteByIds(idList);
        // 批量添加套餐关联菜品，先为其ID赋值，再添加
        setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmealDTO.getId());});
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 设置套餐起售状态，本质上是update
     * @param id
     * @param status
     */
    @Override
    public void startOrStop(Long id, Integer status) {
        // 起售套餐时，需要判断是否有停售的菜品，如果有，则无法起售
        if (status == StatusConstant.ENABLE){
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if (!CollectionUtils.isEmpty(dishes)){
                for (Dish dish : dishes){
                    if (dish.getStatus() == StatusConstant.DISABLE){
                        throw new
                                SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }
        // 根据ID更新状态
        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        setmealMapper.update(setmeal);
    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }


    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
