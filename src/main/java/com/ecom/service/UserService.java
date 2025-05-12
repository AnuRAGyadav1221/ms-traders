package com.ecom.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.UserDtls;

public interface UserService {

    public UserDtls saveUser(UserDtls user);

    public UserDtls saveAdmin(UserDtls user);
    
    public UserDtls getUserByEmail(String email);

    public List<UserDtls> getUsers(String role);

    public Boolean updateAccStatus(Integer id, Boolean status);

    public void incrseFailedAttempt(UserDtls user);

    public void userAccLock(UserDtls user);

    public boolean unlockAccTimeExp(UserDtls user);

    public void updateUserResetToken(String email, String resetToken);

    public UserDtls getUserByToken(String token);

    public UserDtls updateUser(UserDtls user);

    public UserDtls updateUserProfile(UserDtls user, MultipartFile img);

    public Boolean existsEmail(String email);

}
