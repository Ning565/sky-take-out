package com.star.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.star.constant.MessageConstant;
import com.star.constant.StatusConstant;
import com.star.service.DishService;
import com.star.dto.DishDTO;
import com.star.dto.DishPageQueryDTO;
import com.star.entity.Dish;
import com.star.entity.DishFlavor;
import com.star.exception.DeletionNotAllowedException;
import com.star.mapper.DishFlavorMapper;
import com.star.mapper.DishMapper;
import com.star.mapper.SetmealDishMapper;
import com.star.result.PageResult;
import com.star.utils.CacheClient;
import com.star.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CacheClient cacheClient;

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

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 用VO封装结果
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     */
    @Override
    @Transactional
    public void deletByIds(List<Long> ids) {
        // 1.判断不能删除的菜品：起售中的 + 被套餐关联的
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 当前菜品起售中，无法删除
                throw new
                        DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断是否被套餐相关联，上面的需要获取status，这里只需要看有没有数值
        List<Long> setmealDishIds = setmealDishMapper.getByDishIds(ids);
        if (!CollectionUtils.isEmpty(setmealDishIds)) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 2. 删除菜品
        dishMapper.deleteByIds(ids);
        // 3. 删除菜品口味数据
        dishFlavorMapper.deleteByDishIds(ids);


    }

    /**
     * 按照ID查询回显 需要查询dish和口味
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getById(Long id) {
        // 查询菜品数据
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.getById(id);
        // 根据菜品ID查询口味数据，为List集合
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        BeanUtils.copyProperties(dish, dishVO);
        // 拷贝完属性直接设置Flavors即可
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 1.更新菜品表
        dishMapper.update(dish);
        // 2.更新口味表：先删再插
        // 2.1删除
        List<Long> idList = new ArrayList<>();
        idList.add(dishDTO.getId());
        dishFlavorMapper.deleteByDishIds(idList);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (!CollectionUtils.isEmpty(flavors)) {
            // 2.2 为每个新增的flavor的dishId设置为 1
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
        }
        dishFlavorMapper.insertBatch(flavors);
    }

    /**
     * 菜品起售/停售状态设置
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder().status(status).id(id).build();
        dishMapper.update(dish);
    }

    /**
     * 根据分类ID查询菜品信息
     *
     * @param cateId
     * @return
     */
    @Override
    public List<Dish> getByCateId(Long cateId) {
        List<Dish> dishes = dishMapper.getByCateId(cateId);
        return dishes;
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);
            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }


}
