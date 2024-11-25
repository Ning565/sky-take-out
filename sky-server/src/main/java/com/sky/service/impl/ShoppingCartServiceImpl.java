package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        // 1. 判断商品存在于购物车（利用userid结合setmealid/dishid dish flavor判断）
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 拿到当前登录用户的userid
        shoppingCart.setUserId(BaseContext.getCurrentId());
        // 按照userid和提供的信息查询，后续判断
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        // 2. 购物车有此数据，后续添加，用update: nums + 1
        if (!CollectionUtils.isEmpty(list)) {
            ShoppingCart cart = list.get(0);// 对于相同商品/套餐，传入的数据进行查询，取出的list其实只有一条，第一条即唯一一条数据
            // 取出此条购物车数据，把数量加一
            cart.setNumber(cart.getNumber() + 1); // 把当前ShoppingCart类型的cart中的数量加一，然后执行update语句
            shoppingCartMapper.updateNumById(cart);
        } else {
            // 3.第一次添加，用insert，插入购物车数据
            // 判断这次提交的DTO是菜品还是套餐，进而查询他们的信息，添加到购物车表吗，利用前面的shoppingCart对象即可
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 查询菜品信息
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice()); // 单价
            } else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice()); // 单价
            }
            // 统一设置数量，创建时间和插入数据 insert

            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }
}
