package com.ecom.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

public class RazorpaySignatureUtil {

    public static String calculateHMAC(String data, String secret) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hmacBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static boolean isValidSignature(String razorpayOrderId, String paymentId, String razorpaySignature, String secret) throws Exception {
        String payload = razorpayOrderId + "|" + paymentId;
        String generatedSignature = calculateHMAC(payload, secret);
        // Log for debugging
        System.out.println("Payload: " + payload);
        System.out.println("Generated Signature: " + generatedSignature);
        System.out.println("Razorpay Signature  : " + razorpaySignature);

        return generatedSignature.equals(razorpaySignature);
    }
}
