package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.ecom.model.UserDtls;
import com.ecom.repository.RefundAuditRepository;
import com.ecom.service.OrderService;
import com.ecom.service.UserService;

import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {
    private final OrderService orderService;

    public CommonUtil(@Lazy OrderService orderService) {
        this.orderService = orderService;
    }

    @Autowired
    private RefundAuditRepository refundAuditRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Lazy
    @Autowired
    private UserService userService;

    @Value("${application.s3.bucket.category}")
    private String categoryBucket;

    @Value("${application.s3.bucket.product}")
    private String productBucket;

    @Value("${application.s3.bucket.profile}")
    private String profileBucket;

    @Value("${application.s3.bucket.banner}")
    private String bannerBucket;

    public Boolean sendMail(String url, String reciepentEmail) throws UnsupportedEncodingException, MessagingException{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("anuragsinghy001@gmail.com","Shopping Cart");
        helper.setTo(reciepentEmail);
        helper.setSubject("Reset Your Password");
        
        String content = "<p>Hello,</p>"
                    + "<p>You have requested to reset your password.</p>"
                    + "<p>Click the link below to change your password:</p>"
                    + "<p><a href=\"" + url + "\">Reset Password</a></p>"
                    + "<p>If you did not request this, please ignore this email.</p>";
        
        helper.setText(content,true); // true to remove HTML tags
        mailSender.send(message);
        return true;
    }

    public static String generateURL(HttpServletRequest request) {
        
        // http://localhost:8080/forgotPassword
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }
    
    public Boolean sendMailForProduct(ProductOrder order, String statusCode) throws Exception{
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message,true);

        helper.setFrom(new InternetAddress("anuragsinghy001@gmail.com","Shopping Cart"));
        System.out.println("🔍 Checking email: " + order.getOrderAddress().getEmail());
        helper.setTo(order.getOrderAddress().getEmail());
        helper.setSubject("Product Order Status");

        String orderMsg = "<!DOCTYPE html>"
        + "<html>"
        + "<head>"
        + "<style>"
        + "body {font-family: Arial, sans-serif; color: #333; line-height: 1.6;}"
        + ".container {width: 80%; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #f9f9f9;}"
        + ".header {background-color: #007bff; padding: 10px 20px; border-radius: 8px 8px 0 0; color: white; font-size: 18px;}"
        + ".content {padding: 20px;}"
        + ".footer {margin-top: 20px; font-size: 14px; color: #555;}"
        + ".details-table {width: 100%; border-collapse: collapse; margin-top: 15px;}"
        + ".details-table th, .details-table td {padding: 10px; border: 1px solid #ddd; text-align: left;}"
        + ".details-table th {background-color: #f2f2f2;}"
        + "</style>"
        + "</head>"
        + "<body>"
        + "<div class='container'>"
        + "<div class='header'>"
        + "Order Confirmation - E-Shop"
        + "</div>"
        + "<div class='content'>"
        + "<p>Dear [[name]],</p>"
        + "<p>Thank you for shopping with <strong>Ms SAMYABHI TRADERS</strong>! We're excited to confirm your order. Below are the details:</p>"
        
        + "<table class='details-table'>"
        + "<tr><th>Order ID</th><td>[[orderId]]</td></tr>"
        + "<tr><th>Product Name</th><td>[[productName]]</td></tr>"
        + "<tr><th>Category</th><td>[[category]]</td></tr>"
        + "<tr><th>Quantity</th><td>[[quantity]]</td></tr>"
        + "<tr><th>Price</th><td>&#8377; [[price]]</td></tr>"
        + "<tr><th>Payment Type</th><td>[[paymentType]]</td></tr>"
        + "<tr><th>Order Status</th><td>[[status]]</td></tr>"
        + "</table>";
        List<RefundAudit> refundOpt = refundAuditRepository.findByProductOrder(order);
        if (refundOpt.isEmpty()) {
            RefundAudit refund = refundOpt.get(0);
            String refundBlock = "<h4 style='margin-top:30px;'>Refund Details</h4>"
                    + "<table class='details-table'>"
                    + "<tr><th>Refund ID</th><td>" + refund.getRazorpayRefundId() + "</td></tr>"
                    + "<tr><th>Refund Amount</th><td>&#8377; " + refund.getRefundAmount() + "</td></tr>"
                    + "<tr><th>Refund Time</th><td>" + refund.getRefundTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "</td></tr>"
                    + "</table>";

            orderMsg += refundBlock;
        }

        orderMsg += "<p>If you have any questions or need assistance, feel free to contact our support team.</p>"
        + "<p>We hope to serve you again soon!</p>"

        + "<div class='footer'>"
        + "<p>Best regards,<br><strong>The Ms SAMYABHI TRADERS Team</strong></p>"
        + "</div>"

        + "</div>"
        + "</div>"
        + "</body>"
        + "</html>";

        orderMsg = orderMsg.replace("[[name]]", order.getOrderAddress().getFirstName() + " " + order.getOrderAddress().getLastName());
        orderMsg = orderMsg.replace("[[orderId]]", order.getOrderId());
        orderMsg = orderMsg.replace("[[productName]]", order.getProduct().getTitle());
        orderMsg = orderMsg.replace("[[category]]", order.getProduct().getCategory());
        orderMsg = orderMsg.replace("[[quantity]]", order.getQuantity().toString());
        orderMsg = orderMsg.replace("[[price]]", order.getPrice().toString());
        orderMsg = orderMsg.replace("[[paymentType]]", order.getPaymentType());
        orderMsg = orderMsg.replace("[[status]]", statusCode);


        helper.setText(orderMsg,true); // true to remove HTML tags
        List<RefundAudit> refundAudits = orderService.getRefundAuditByOrder(order);
        RefundAudit refund = refundAudits.isEmpty() ? null : refundAudits.get(0);
        
        byte[] pdfData = InvoicePdfGenerator.generateInvoicePDF(order,refund);
        helper.addAttachment("Order_" + order.getOrderId() + "_Receipt.pdf", 
        new ByteArrayDataSource(pdfData, "application/pdf"));
    
        System.out.println("📩 Sending mail to: " + order.getOrderAddress().getEmail());
        System.out.println("📦 Product: " + order.getProduct().getTitle());

        try {
            mailSender.send(message);
            System.out.println("Email sent successfully to " + order.getOrderAddress().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw e;
        }
        
        return true;
    }

    public UserDtls getLoggedUserDtls(Principal p) {
        String email = p.getName();
        UserDtls userDtls = userService.getUserByEmail(email);

        return userDtls;
    }

    public String getImageUrl(MultipartFile file, Integer bucketType){
        String bucketName = null;

        if(bucketType == 1){
            bucketName = categoryBucket;
        }else if(bucketType == 2){
            bucketName = productBucket;
        }else if(bucketType == 3){
            bucketName = profileBucket;
        }else{
            bucketName = bannerBucket;
        }

        String imageName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : "default.jpg";

        // https://mstrader-category.s3.ap-south-1.amazonaws.com/133520442166531896.jpg
        String url = "https://" + bucketName+".s3.ap-south-1.amazonaws.com/"+ imageName;
        return url;
    }
}

