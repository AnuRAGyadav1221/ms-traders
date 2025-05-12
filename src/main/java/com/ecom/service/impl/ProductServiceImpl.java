package com.ecom.service.impl;

// import java.io.File;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Product;
import com.ecom.repository.ProductRepository;
import com.ecom.service.FileService;
import com.ecom.service.ProductService;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;

@Service
public class ProductServiceImpl implements ProductService{
    // @Value("${file.upload-dir}")
    // private String uploadDir;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    private FileService fileService;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    

    @Override
    public Page<Product> getAllProductPagination(Integer pageNo, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findAll(pageable);
    }

    @Override
    public Boolean deleteProduct(Integer id) {
        Product product = productRepository.findById(id).orElse(null);

        if(!ObjectUtils.isEmpty(product)){
            productRepository.delete(product);
            return true;
        }
        return false;
    }

    @Override
    public Product getProductById(Integer id) {
    
       Product product = productRepository.findById(id).orElse(null);
       return product;
    }
    @Override
    public Product updateProduct(Product product, MultipartFile image) {

        Product dbProduct = getProductById(product.getId());
        
        // String imageName = image.isEmpty() ? dbProduct.getImage() : image.getOriginalFilename();
        String imageUrl = commonUtil.getImageUrl(image, BucketType.PRODUCT.getId());

        dbProduct.setTitle(product.getTitle());
        dbProduct.setDescription(product.getDescription());
        dbProduct.setCategory(product.getCategory());
        dbProduct.setPrice(product.getPrice());
        dbProduct.setStock(product.getStock());
        dbProduct.setImage(imageUrl);
        dbProduct.setIsActive(product.getIsActive());

        dbProduct.setDiscount(product.getDiscount());
        //100*(5/100) = 5 // 100 -5 = 95
        Double discountPrice = product.getPrice() - product.getPrice() * (product.getDiscount() / 100.0);
        dbProduct.setDiscountPrice(discountPrice);

        // Save updated product to the database
        Product updateProduct = productRepository.save(dbProduct);
    
        if (!ObjectUtils.isEmpty(updateProduct)) {
            if (!image.isEmpty()) {
                try {
                //     File saveDir = new File(uploadDir + "product_img/");
                // if (!saveDir.exists()) saveDir.mkdirs();

                // Path path = Paths.get(saveDir.getAbsolutePath(), imageName);
                // Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                fileService.uploadFileS3(image,BucketType.PRODUCT.getId());

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to save image file: " + e.getMessage());
                }
                
            }
            return updateProduct;
        }
        return null;
    }

    @Override
    public List<Product> searchList(String ch) {
        List<Product> searchProductList = productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch,ch);
        return searchProductList;
    }

    @Override
    public Page<Product> searchListPagination(Integer pageNo, Integer pageSize, String ch) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch,ch,pageable);
    }

    @Override
    public List<Product> getAllActiveProducts(String category) {
        List<Product> products = null;
        if(ObjectUtils.isEmpty(category)){
            products = productRepository.findByIsActiveTrue();
        }else{
            products = productRepository.findByCategory(category);
        }
        return products;
    }

    @Override
    public Page<Product> getAllActiveProductPagination(Integer pageNo, Integer pageSize, String category) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Product> pageProduct = null;

        if(ObjectUtils.isEmpty(category)){
            pageProduct = productRepository.findByIsActiveTrue(pageable);
        }else{
            pageProduct = productRepository.findByCategory(pageable, category);
        }
        return pageProduct;

    }

    @Override
    public Page<Product> searchActiveProductPagination(Integer pageNo, Integer pageSize, String category,String ch) {

        Page<Product> pageProduct = null;

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        pageProduct = productRepository.findByisActiveTrueAndTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(ch,ch,pageable);

        // if(ObjectUtils.isEmpty(category)){
        //     pageProduct = productRepository.findByIsActiveTrue(pageable);
        // }else{
        //     pageProduct = productRepository.findByCategory(pageable, category);
        // }
        return pageProduct;
    }
    
}
