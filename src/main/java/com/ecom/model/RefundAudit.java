package com.ecom.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razorpayRefundId;

    private Double refundAmount;

    private LocalDateTime refundTime;

    @ManyToOne
    private ProductOrder productOrder;

}