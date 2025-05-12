package com.ecom.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.AddToCart;
import com.ecom.model.Category;
import com.ecom.model.OrderRequest;
import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.ecom.model.UserDtls;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.repository.RefundAuditRepository;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;
import com.ecom.util.CommonUtil;
import com.ecom.util.InvoicePdfGenerator;
import com.ecom.util.OrderStatus;
import com.ecom.util.RazorpaySignatureUtil;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/user")
public class UserController {
    
    @Autowired    
    private RefundAuditRepository refundAuditRepository;

    @Autowired
    private ProductOrderRepository productOrderRepository;

    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
	private CommonUtil commonUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${razorpay.key}")
    private String razorpayKey;

    @Value("${razorpay.secret}")
    private String razorpaySecret;


    @ModelAttribute
	public void getUserDetails(Principal p, Model m){
		if(p != null){
			String email = p.getName();
			UserDtls user = userService.getUserByEmail(email);
			m.addAttribute("user", user);
            Integer countCart = cartService.getCountCart(user.getId());
			m.addAttribute("countCart", countCart);
		}

        List<Category> allActiveACategories = categoryService.getAllActiveCategories();
		m.addAttribute("categories", allActiveACategories);
	}
    

    @GetMapping("/addCart")
    public String addToCart(@RequestParam Integer pid, @RequestParam Integer uid,
    HttpSession session){
        AddToCart savedCart = cartService.saveCart(pid, uid);
        if(ObjectUtils.isEmpty(savedCart)){
            session.setAttribute("errorMsg", "Product add to cart failed!!");
        }else{
            session.setAttribute("sucMsg", "Product added to cart");
        }
        return "redirect:/product_view/" + pid;
    }

    @GetMapping("/cart")
    public String loadCartPage(Principal p, Model m){
      
        UserDtls userDtls = commonUtil.getLoggedUserDtls(p);
        List<AddToCart> carts = cartService.getCartsByUSer(userDtls.getId());
        m.addAttribute("cartList", carts);
        Double totalOrderPrice = 0.0;
        if (!carts.isEmpty()) {
            totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
            m.addAttribute("totalOrderPrice", totalOrderPrice);
        }
        return "user/cart";
    
    }

    @GetMapping("/cartQuantityUpd")
    public String updateCartQuantity(@RequestParam String sy, @RequestParam Integer cartId){
        cartService.updateQuantity(sy,cartId);
        return "redirect:/user/cart";
    }

    @GetMapping("/orders")
    public String orderPage(Principal p, Model m){
        UserDtls userDtls = commonUtil.getLoggedUserDtls(p); 
        List<AddToCart> carts = cartService.getCartsByUSer(userDtls.getId());
        m.addAttribute("cartList", carts);
        Double totalOrderPrice = 0.0;
        if (!carts.isEmpty()) {
            totalOrderPrice = carts.get(carts.size() - 1).getTotalOrderPrice();
            m.addAttribute("totalOrderPrice", totalOrderPrice);
            m.addAttribute("Delivery_fee", orderService.calculateDeliveryFee(totalOrderPrice));
            m.addAttribute("Tax", orderService.calculateTax(totalOrderPrice));
        }
        return "user/order";
    }

    @PostMapping("/save-order")
    public String saveOrder(@ModelAttribute OrderRequest request, Principal p, Model model,HttpSession session) throws Exception{
        try {
            UserDtls user = commonUtil.getLoggedUserDtls(p);
            if ("ONLINE".equalsIgnoreCase(request.getPaymentType())) {
                session.setAttribute("orderRequest", request);
                // Call Razorpay order creation here
                Map<String, Object> orderDetails = orderService.createRazorPayOrder(user.getId(), request);
                
                // Pass data to frontend for Razorpay checkout
                model.addAttribute("razorpayKey", razorpayKey);
                model.addAttribute("razorpayOrderId", orderDetails.get("id"));

                model.addAttribute("amount", orderDetails.get("amount"));
                model.addAttribute("currency", "INR");
                model.addAttribute("userId", user.getId());
                model.addAttribute("userName", request.getFirstName() + " "+ request.getLastName());
                model.addAttribute("userEmail", request.getEmail());
                model.addAttribute("userPhone", request.getMobileNo());
        
                return "user/online_payment";
            }else{
                // For COD
                orderService.saveOrder(user.getId(), request,null,null,null,null);
                session.setAttribute("msg", "Order placed successfully via Cash on Delivery!");
                return "/user/success";
            }
        } catch (Exception e) {            
            e.printStackTrace();
            session.setAttribute("error", "Failed to place order: " + e.getMessage());
            return "redirect:/user/orders";
        }
        
    }

    @PostMapping("/payment-success")
    @ResponseBody
    public ResponseEntity<String> paymentSuccess(@RequestBody Map<String, Object> data, Principal principal, HttpSession session) throws Exception {
        try{
            String payOrderId = (String) data.get("razorpay_order_id");
            String payPaymentId = (String) data.get("razorpay_payment_id");
            String paySignature = (String) data.get("razorpay_signature");
            Object paidAmountObj = data.get("paidAmount");
            if (paidAmountObj == null) {
                return ResponseEntity.badRequest().body("Paid amount is missing");
            }
        
            double paidAmount = Double.parseDouble(paidAmountObj.toString());

            OrderRequest orderRequest = (OrderRequest) session.getAttribute("orderRequest");
            if (orderRequest == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session expired or invalid order data");
            }

            UserDtls user = commonUtil.getLoggedUserDtls(principal);
            Integer userId = user.getId();

            boolean verified = RazorpaySignatureUtil.isValidSignature(payOrderId, payPaymentId, paySignature, razorpaySecret);

            if (verified) {
                orderService.saveOrder(userId, orderRequest,payOrderId,payPaymentId,paySignature,paidAmount);
                return ResponseEntity.ok("Order saved");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception Message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment");
        }
    }

    @GetMapping("/success")
    public String loadSuccess(){
        return "user/success";
    }

    @GetMapping("/user-orders")
    public String myOrder(Model m, Principal p) {
        if (p == null) {
            return "redirect:/login";
        }
        Integer userId = commonUtil.getLoggedUserDtls(p).getId();
        List<ProductOrder> ordersList = orderService.getOrders(userId);

        System.out.println("Orders: " + ordersList);
        System.out.println("Order List: " + ordersList.size());

        Map<Integer, Boolean> refundedOrderMap = ordersList.stream().collect(
            java.util.stream.Collectors.toMap(
                ProductOrder::getId,
                order -> refundAuditRepository.findByProductOrder(order).isEmpty()
            )
        );

        m.addAttribute("ordersList", ordersList);
        m.addAttribute("refundedOrderMap", refundedOrderMap);
        return "/user/my_orders";
    }

    @Transactional
    @PostMapping("/update-status")
    public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st, HttpSession session) {
        OrderStatus[] statusValues = OrderStatus.values();
        String status = null;

        for(OrderStatus stat : statusValues) {
            if(stat.getId().equals(st)) {
                status = stat.getName();
                break;
            }
        }
        ProductOrder updateOrder = orderService.updateOrderStatus(id, status);
        if (updateOrder == null) {
            session.setAttribute("errorMsg", "Order not found");
            return "redirect:/user/user-orders";
        }

        if (OrderStatus.CANCEL.getName().equals(status) && "ONLINE".equalsIgnoreCase(updateOrder.getPaymentType())) {
            try {
                List<RefundAudit> existingRefund = refundAuditRepository.findByProductOrder(updateOrder);
                if (!existingRefund.isEmpty()) {
                    session.setAttribute("errorMsg", "Refund already processed for this product.");
                    return "redirect:/user/user-orders";
                }

                // Logic for refunding amount
                List<ProductOrder> relatedOrders = orderService.getOrdersByPayOrderId(updateOrder.getPayOrderId());
                double totalProductPrice = relatedOrders.stream()
                    .mapToDouble(o -> o.getPrice() * o.getQuantity()).sum();
                double totalTax = relatedOrders.get(0).getTax();
                double totalDelivery = relatedOrders.get(0).getDeliveryFee();
                double canceledPrice = updateOrder.getPrice() * updateOrder.getQuantity();

                // Proportional tax and delivery
                double proportionalTax = (canceledPrice / totalProductPrice) * totalTax;
                double proportionalDelivery = (canceledPrice / totalProductPrice) * totalDelivery;
                double refundAmount = canceledPrice + proportionalTax + proportionalDelivery;

                // Perform refund and capture Razorpay Refund ID
                String razorpayRefundId = orderService.refundPaymentAndReturnRefundId(updateOrder.getPayPaymentId(), refundAmount);

                // Save Refund Audit entry
                RefundAudit audit = new RefundAudit();
                audit.setProductOrder(updateOrder);
                audit.setRazorpayRefundId(razorpayRefundId);
                audit.setRefundAmount(refundAmount);
                audit.setRefundTime(LocalDateTime.now());

                refundAuditRepository.save(audit);
                session.setAttribute("sucMsg", "Refund of ₹" + refundAmount + " successful!");
            } catch (Exception e) {
                e.printStackTrace();
                session.setAttribute("errorMsg", "Refund failed: " + e.getMessage());
                return "redirect:/user/user-orders";
            }
        }

        try {
            commonUtil.sendMailForProduct(updateOrder, status);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!ObjectUtils.isEmpty(updateOrder)) {
            session.setAttribute("sucMsg", "Status Updated");
        } else {
            session.setAttribute("errorMsg", "Status not Updated");
        }

        return "redirect:/user/user-orders";
    }

    @GetMapping("/profile")
    public String profile() {
        return "/user/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session){
        UserDtls updateUserProfile = userService.updateUserProfile(user, img);

        if(ObjectUtils.isEmpty(updateUserProfile)){ 
            session.setAttribute("errorMsg", "Profile not Updated");
        }else{
            session.setAttribute("sucMsg", "Profile Updated Successfully!");
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String newPassword, 
        @RequestParam String curPassword,
        HttpSession session,
        Principal p){
        
        UserDtls loggedUser = commonUtil.getLoggedUserDtls(p);
        boolean matches = passwordEncoder.matches(curPassword, loggedUser.getPassword());
        
        if(matches){ 

            String encode = passwordEncoder.encode(newPassword);
            loggedUser.setPassword(encode);
            UserDtls updateUser = userService.updateUser(loggedUser);
            if (ObjectUtils.isEmpty(updateUser)) {
                session.setAttribute("errorMsg", "Password not updated!! Error in server");
            } else {
                session.setAttribute("sucMsg", "Password Updated Successfully");
            }

        }else{
            session.setAttribute("errorMsg", "Current Password is incorrect");
        }
        return "redirect:/user/profile";
    }

    @GetMapping("/invoice/{orderId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String orderId) throws Exception {

        List<ProductOrder> orders = productOrderRepository.findByOrderId(orderId);
        System.out.println("Received Order ID: " + orderId);
        if (orders == null || orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Order with ID " + orderId + " not found").getBytes());
        }

        ProductOrder order = orders.get(0);
        List<RefundAudit> refunds = orderService.getRefundAuditByOrder(order); 
        RefundAudit refund = (refunds != null && !refunds.isEmpty()) ? refunds.get(0) : null;         
        byte[] pdfData = InvoicePdfGenerator.generateInvoicePDF(order, refund);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("invoice_" + order.getOrderId() + ".pdf").build());

        return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);
    }

        
}
