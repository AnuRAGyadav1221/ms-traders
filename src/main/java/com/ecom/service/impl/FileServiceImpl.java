package com.ecom.service.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.ecom.service.FileService;

@Service
public class FileServiceImpl implements FileService{

    @Autowired
    public AmazonS3 amazonS3;
    
    @Value("${application.s3.bucket.category}")
    private String categoryBucket;

    @Value("${application.s3.bucket.product}")
    private String productBucket;

    @Value("${application.s3.bucket.profile}")
    private String profileBucket;

    @Value("${application.s3.bucket.banner}")
    private String bannerBucket;

    @Override
    public Boolean uploadFileS3(MultipartFile file, Integer bucketType) {
        String bucketName = null;

        try{
            if(bucketType == 1){
                bucketName = categoryBucket;
            }else if(bucketType == 2){
                bucketName = productBucket;
            }else if(bucketType == 3){
                bucketName = profileBucket;
            }else{
                bucketName = bannerBucket;
            }
            String fileName = file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.setContentType(file.getContentType());
            objMetadata.setContentLength(file.getSize());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,fileName,inputStream,objMetadata);
            PutObjectResult saveObject = amazonS3.putObject(putObjectRequest);

            if(!ObjectUtils.isEmpty(saveObject)){
                return true;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}
