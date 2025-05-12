package com.ecom.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.razorpay.RazorpayException;

public interface OrderService {

    public void saveOrder(Integer userId, OrderRequest orderRequest,
    String payOrderId, String payPaymentId,
    String paySignature, Double paidAmount) throws Exception;

    public List<ProductOrder> getOrders(Integer userId);

    public ProductOrder updateOrderStatus(Integer id, String status);

    public List<ProductOrder> getAllOrders();

    public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize);

    public ProductOrder getOrdersByOrderId(String orderId);

    public Map<String, Object> createRazorPayOrder(Integer id, OrderRequest request) throws RazorpayException;

    public Double calculateDeliveryFee(Double totalOrderPrice);

    public Double calculateTax(Double totalOrderPrice);

    public String refundPayment(String paymentId, Double refundAmount);

    public List<ProductOrder> getOrdersByPayOrderId(String payOrderId);

    public String refundPaymentAndReturnRefundId(String payPaymentId, double refundAmount) throws RazorpayException;

    List<RefundAudit> getRefundAuditByOrder(ProductOrder order);

}
