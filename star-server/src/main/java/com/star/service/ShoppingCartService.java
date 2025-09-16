package com.star.service;

import com.star.dto.ShoppingCartDTO;
import com.star.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {


    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> showShoopingCart();

    void clean();
}
