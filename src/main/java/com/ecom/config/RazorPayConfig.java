package com.ecom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorPayConfig {

    private String razorKey;
    
    private String razorSecret;
    
}
