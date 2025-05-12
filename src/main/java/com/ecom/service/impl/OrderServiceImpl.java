package com.ecom.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.model.AddToCart;
import com.ecom.model.OrderAddress;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.ecom.repository.CartRepository;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.RefundAuditRepository;
import com.ecom.service.CartService;
import com.ecom.service.OrderService;
import com.ecom.util.CommonUtil;
import com.ecom.util.OrderStatus;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;


@Service
public class OrderServiceImpl implements OrderService{
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final CommonUtil commonUtil;
    
    public OrderServiceImpl(@Lazy CommonUtil commonUtil) {
        this.commonUtil = commonUtil;
    }

    @Autowired    
    private RefundAuditRepository refundAuditRepository;
    
    @Autowired
    private CartService cartService;

    @Autowired
    private ProductOrderRepository productOrderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Transactional
    @Override
    public void saveOrder(Integer userId, OrderRequest orderRequest,
                          String payOrderId, String payPaymentId,
                          String paySignature, Double paidAmount) throws Exception {
    
        if (userId == null || orderRequest == null) {
            throw new IllegalArgumentException("Invalid user ID or order request.");
        }
    
        List<AddToCart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            throw new Exception("Cart is empty. Cannot place order.");
        }
    
        Double totalPrice = carts.stream()
            .mapToDouble(cart -> cart.getProduct().getDiscountPrice() * cart.getQuantity())
            .sum();
        Double tax = calculateTax(totalPrice);
        Double deliveryFee = calculateDeliveryFee(totalPrice);
        Double grandTotal = totalPrice + tax + deliveryFee;

        for (AddToCart cart : carts) {
            ProductOrder order = new ProductOrder();
            order.setOrderId(UUID.randomUUID().toString());
            order.setOrderDate(LocalDate.now());
    
            order.setProduct(cart.getProduct());
            order.setPrice(cart.getProduct().getDiscountPrice());
            order.setQuantity(cart.getQuantity());
            order.setUser(cart.getUser());
    
            order.setStatus(OrderStatus.IN_PROGRESS.getName());
            order.setPaymentType(orderRequest.getPaymentType());
    
            // ✅ Only set Razorpay fields for "Online" payment
            if ("Online".equalsIgnoreCase(orderRequest.getPaymentType())) {
                if (payOrderId != null) order.setPayOrderId(payOrderId);
                if (payPaymentId != null) order.setPayPaymentId(payPaymentId);
                if (paySignature != null) order.setPaySignature(paySignature);
                if (paidAmount != null) order.setPaidAmount(paidAmount);
            }
    
            // 🏠 Set address
            OrderAddress address = new OrderAddress();
            address.setFirstName(orderRequest.getFirstName());
            address.setLastName(orderRequest.getLastName());
            address.setEmail(orderRequest.getEmail());
            address.setMobileNo(orderRequest.getMobileNo());
            address.setAddress(orderRequest.getAddress());
            address.setCity(orderRequest.getCity());
            address.setState(orderRequest.getState());
            address.setPincode(orderRequest.getPincode());
            order.setOrderAddress(address);

            order.setTax(tax);
            order.setDeliveryFee(deliveryFee);
            order.setGrandTotal(grandTotal);
    
            try {
                ProductOrder savedOrder = productOrderRepository.save(order);
                try {
                    commonUtil.sendMailForProduct(savedOrder, "Successfully");
                } catch (Exception e) {
                    log.warn("Mail failed for orderId {}", savedOrder.getOrderId(), e);
                }
            } catch (Exception e) {
                log.error("Error saving order for userId {}: {}", userId, e.getMessage());
                throw e;
            }
        }
    
        cartRepository.deleteAll(carts);
        log.info("Order placed and cart cleared for userId {}", userId);
    }
    

    @Override
    public List<ProductOrder> getOrders(Integer userId) {
        List<ProductOrder> orders = productOrderRepository.findByUserId(userId);
        return orders;
    }

    @Override
    public ProductOrder updateOrderStatus(Integer id, String status){
        Optional<ProductOrder> productOrder = productOrderRepository.findById(id);
        if(productOrder.isPresent()){
            ProductOrder product = productOrder.get();
            product.setStatus(status);
            ProductOrder updateOrder = productOrderRepository.save(product);
            return updateOrder;
        }
        return null;
    }

    @Override
    public List<ProductOrder> getAllOrders() {
        return productOrderRepository.findAll();
    }

    @Override
    public Page<ProductOrder> getAllOrdersPagination(Integer pageNo, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNo,pageSize);
        return productOrderRepository.findAll(pageable);
    }

    @Override
    public ProductOrder getOrdersByOrderId(String orderId) {
        List<ProductOrder> byOrderId = productOrderRepository.findByOrderId(orderId);
        if (byOrderId != null && !byOrderId.isEmpty()) {
            return byOrderId.get(0);
        }
        return null;
    }

    private double calculateTotalAmount(Integer userId) {
        List<AddToCart> carts = cartService.getCartsByUSer(userId);
        if (!carts.isEmpty()) {
            return carts.get(carts.size() - 1).getTotalOrderPrice(); // Just like your controller
        }
        return 0.0;
    }

    @Override
    public Double calculateDeliveryFee(Double totalAmount) {
        return totalAmount < 1000 ? 250.0 : 0.0;
    }


    @Override
    public Double calculateTax(Double totalOrderPrice) {
        return totalOrderPrice * 0.05; // 5% tax
    }
    
    
    @Override
    public Map<String, Object> createRazorPayOrder(Integer userId, OrderRequest request) throws RazorpayException {
        log.info("Creating RazorPay order for userId {}", userId);

        RazorpayClient client = new RazorpayClient(key, secret);

        Double totalAmount = calculateTotalAmount(userId); // use your logic to get final amount
        Double deliveryFee = calculateDeliveryFee(totalAmount);
        Double tax = calculateTax(totalAmount);
        double grandTotal = totalAmount + deliveryFee + tax;
        JSONObject options = new JSONObject();
        options.put("amount", (int)(grandTotal *100));
        options.put("currency", "INR");
        options.put("receipt", request.getEmail());
        options.put("payment_capture", 1);

        Order order = client.orders.create(options);
        return order.toJson().toMap();
    }

    @Override
    public String refundPayment(String paymentId, Double refundAmount) {
        if (paymentId == null || paymentId.isEmpty()) {
            return "Invalid payment ID.";
        }
    
        if (refundAmount == null || refundAmount <= 0) {
            return "Invalid refund amount.";
        }
        try {
            RazorpayClient client = new RazorpayClient(key, secret);

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (int)(refundAmount * 100));

            com.razorpay.Refund refund = client.payments.refund(paymentId, refundRequest);

            log.info("Refund initiated: {}", refund.toString());
            return "Refund successful with ID: " + refund.get("id");
        } catch (RazorpayException e) {
            log.error("Refund failed: {}", e.getMessage());
            return "Refund failed: " + e.getMessage();
        }
    }


    @Override
    public List<ProductOrder> getOrdersByPayOrderId(String payOrderId) {
        return productOrderRepository.findByPayOrderId(payOrderId);
    }


    @Override
    public String refundPaymentAndReturnRefundId(String paymentId, double refundAmount) throws RazorpayException{
        RazorpayClient razorpay = new RazorpayClient(key, secret);

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("payment_id", paymentId);
        refundRequest.put("amount", (int)(refundAmount * 100)); // in paise

        Refund refund = razorpay.refunds.create(refundRequest);

        // Log refund details
        log.info("Refund Successful: ID = {}", (String)refund.get("id"));

        // Create and save RefundAudit entry
        ProductOrder productOrder = getProductOrderByPaymentId(paymentId);  // You need to implement this method
        RefundAudit refundAudit = RefundAudit.builder()
                .razorpayRefundId(refund.get("id").toString())
                .refundAmount(refundAmount)
                .refundTime(LocalDateTime.now())
                .productOrder(productOrder)
                .build();

        refundAuditRepository.save(refundAudit);
        return "Refund Successful: ID = " + refund.get("id"); 
    }


    private ProductOrder getProductOrderByPaymentId(String paymentId) {
        List<ProductOrder> orders = productOrderRepository.findByPayPaymentId(paymentId);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Order not found for payment ID: " + paymentId);
        }
        return orders.get(0);
    }


    @Override
    public List<RefundAudit> getRefundAuditByOrder(ProductOrder order) {
        return refundAuditRepository.findByProductOrder(order);
    }

    

}
