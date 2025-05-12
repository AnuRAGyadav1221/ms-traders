package com.ecom.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.ecom.model.AddToCart;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductRepository;
import com.ecom.repository.UserRepository;
import com.ecom.service.CartService;

@Service
public class CartServiceImpl implements CartService{
    
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<AddToCart> getCartsByUSer(Integer userId) {
        List<AddToCart> cartList = cartRepository.findByUserId(userId);

        Double totalOrderPrice = 0.0;

        List<AddToCart> updatedCart = new ArrayList<>();

        for(AddToCart cart : cartList){
            Double totalPrice = (cart.getProduct().getDiscountPrice() * cart.getQuantity());
            cart.setTotalPrice(totalPrice);
            totalOrderPrice += totalPrice;
            cart.setTotalOrderPrice(totalOrderPrice);
            updatedCart.add(cart);
        }

        return updatedCart;
    }

    @Override
    public AddToCart saveCart(Integer productId, Integer userId) {

        UserDtls userDetails = userRepository.findById(userId).get();
        Product productDetails = productRepository.findById(productId).get();

        AddToCart cartStatus = cartRepository.findByProductIdAndUserId(productId, userId);

        AddToCart cart = null;
        if(ObjectUtils.isEmpty(cartStatus)){
            cart = new AddToCart();
            cart.setProduct(productDetails);
            cart.setUser(userDetails);
            cart.setQuantity(1);
            cart.setTotalPrice(1 * productDetails.getDiscountPrice());
        }else{
            cart = cartStatus;
            cart.setQuantity(cartStatus.getQuantity() + 1);
            cart.setTotalPrice(cartStatus.getQuantity() * cart.getProduct().getDiscountPrice()); 
        }
        AddToCart saveCart = cartRepository.save(cart);
        
        return saveCart;
    }

    @Override
    public Integer getCountCart(Integer userId) {
        Integer countUser = cartRepository.countByUserId(userId);
        return countUser;
    }

    @Override
    public void updateQuantity(String sy, Integer cartId) {
        AddToCart cart = cartRepository.findById(cartId).get();

        int updQuantity;
        if(sy.equalsIgnoreCase("de")){
            updQuantity = cart.getQuantity() - 1;
            if(updQuantity <= 0){
                cartRepository.deleteById(cartId);
                return;
            }
        }else{
            updQuantity = cart.getQuantity() + 1;
        }
        cart.setQuantity(updQuantity);
        cartRepository.save(cart);

    }

}
