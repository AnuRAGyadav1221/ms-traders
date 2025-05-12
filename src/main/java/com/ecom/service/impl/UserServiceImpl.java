package com.ecom.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;
import com.ecom.service.FileService;
import com.ecom.service.UserService;
import com.ecom.util.AppConstant;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;


@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public UserDtls saveUser(UserDtls user) {
        user.setRole("ROLE_USER");
        user.setIsEnable(true);
        user.setAccNonLocked(true);
        user.setFailedAttempt(0);
        user.setLockTime(null);
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        UserDtls saveUser = userRepository.save(user);
        return saveUser;
    }

    

    @Override
    public UserDtls saveAdmin(UserDtls user) {
        user.setRole("ROLE_ADMIN");
        user.setIsEnable(true);
        user.setAccNonLocked(true);
        user.setFailedAttempt(0);
        user.setLockTime(null);
        
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        UserDtls saveAdmin = userRepository.save(user);
        return saveAdmin;
    }

    @Override
    public UserDtls getUserByToken(String token) {
        return userRepository.findByResetToken(token);
    }

    @Override
    public UserDtls getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDtls> getUsers(String role) {
        return userRepository.findByRole(role);
    }

    @Override
    public Boolean updateAccStatus(Integer id, Boolean status) {
        Optional<UserDtls> findUser = userRepository.findById(id);

        if(findUser.isPresent()){
            UserDtls userDtls = findUser.get();
            userDtls.setIsEnable(status);
            userRepository.save(userDtls);
            return true;
        }
        return false;
    }

    @Override
    public void incrseFailedAttempt(UserDtls user) {
        int attempt = user.getFailedAttempt() + 1;
        user.setFailedAttempt(attempt);
        userRepository.save(user);
    }

    @Override
    public void userAccLock(UserDtls user) {
        user.setAccNonLocked(false);
        user.setLockTime(new Date());
        userRepository.save(user);
    }

    @Override
    public boolean unlockAccTimeExp(UserDtls user) {
        long lockTime = user.getLockTime().getTime();
        long unLockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;

        long currentTime = System.currentTimeMillis();
        if(unLockTime < currentTime){
            user.setAccNonLocked(true);
            user.setFailedAttempt(0);
            user.setLockTime(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public void updateUserResetToken(String email, String resetToken) {
        UserDtls userEmail = userRepository.findByEmail(email); 
        userEmail.setResetToken(resetToken);
        userRepository.save(userEmail);      
    }

    @Override
    public UserDtls updateUser(UserDtls user) {
        return userRepository.save(user);
    }

    @Override
    public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {
        UserDtls existUser = userRepository.findById(user.getId()).get();

        if(!img.isEmpty()){
            String imageUrl = commonUtil.getImageUrl(img, BucketType.PROFILE.getId());
            existUser.setProfileImage(imageUrl);
        }

        if(!ObjectUtils.isEmpty(existUser)){
            // Update other user details
            existUser.setName(user.getName());
            existUser.setMobileNo(user.getMobileNo());
            existUser.setAddress(user.getAddress());
            existUser.setCity(user.getCity());
            existUser.setState(user.getState());
            existUser.setPincode(user.getPincode());
        }
        try{
            if (!img.isEmpty()) {
                fileService.uploadFileS3(img,BucketType.PROFILE.getId());
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        return userRepository.save(existUser);
    }

    @Override
    public Boolean existsEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
