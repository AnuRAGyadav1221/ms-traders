package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.model.ProductOrder;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Integer>{

    List<ProductOrder> findByUserId(Integer userId);

    List<ProductOrder> findByOrderId(String orderId);

    List<ProductOrder> findByPayOrderId(String payOrderId);

    List<ProductOrder> findByPayPaymentId(String paymentId);
    
}
