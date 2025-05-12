package com.ecom.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.UserService;
import com.ecom.util.AppConstant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthFailureHandlerImpl extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    public UserService userService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
    
        String email = request.getParameter("username");
        UserDtls userDtls = userRepository.findByEmail(email);
    
        if (userDtls != null) {
            if (Boolean.TRUE.equals(userDtls.getIsEnable())) {
                if (Boolean.TRUE.equals(userDtls.getAccNonLocked())) {
                    if (userDtls.getFailedAttempt() < AppConstant.ATTEMPT_TIME) {
                        userService.incrseFailedAttempt(userDtls);
                    } else {
                        userService.userAccLock(userDtls);
                        exception = new LockedException("Your account is locked after 3 failed attempts!");
                    }
                } else {
                    if (userService.unlockAccTimeExp(userDtls)) {
                        exception = new LockedException("Your account is unlocked! Please try to login.");
                    } else {
                        exception = new LockedException("Your account is locked! Please try after some time.");
                    }
                }
            } else {
                exception = new LockedException("Your account is inactive.");
            }
        } else {
            exception = new LockedException("Invalid Email or Password!");
        }
    
        super.setDefaultFailureUrl("/signin?error");
        super.onAuthenticationFailure(request, response, exception);
    }
    
}
