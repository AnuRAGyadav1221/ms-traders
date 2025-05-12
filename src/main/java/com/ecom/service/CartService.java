package com.ecom.service;

import java.util.List;

import com.ecom.model.AddToCart;

public interface CartService {

    public AddToCart saveCart(Integer productId, Integer userId);

    public List<AddToCart> getCartsByUSer(Integer userId);

    public Integer getCountCart(Integer userId);

    public void updateQuantity(String sy, Integer cartId);

}
