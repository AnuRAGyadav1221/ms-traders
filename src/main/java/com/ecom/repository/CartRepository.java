package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.AddToCart;


@Repository
public interface CartRepository extends JpaRepository<AddToCart,Integer>{

    public AddToCart findByProductIdAndUserId(Integer productId, Integer userId);

    public Integer countByUserId(Integer userId);

    public List<AddToCart> findByUserId(Integer userId);

}
