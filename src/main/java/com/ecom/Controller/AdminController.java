package com.ecom.Controller;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.ProductOrder;
import com.ecom.model.RefundAudit;
import com.ecom.model.UserDtls;
import com.ecom.repository.ProductOrderRepository;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.FileService;
import com.ecom.service.OrderService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;
import com.ecom.util.InvoicePdfGenerator;
import com.ecom.util.OrderStatus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductOrderRepository productOrderRepository;
    
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
	private CommonUtil commonUtil;

    @Autowired
    private FileService fileService;

    // @Value("${file.upload-dir}")
    // private String uploadDir;

    // @Value("${file.upload-dir:/tmp/uploads}")
    // private String uploadDir;


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

    @GetMapping("/")
    public String index() {
        return "admin/index";
    }
    
    @GetMapping("/loadAddProduct")
    public String loadAddProduct(Model m) {
        List<Category> categories = categoryService.getAllCategory(); 
        m.addAttribute("categories", categories);
        return "admin/add_product";
    }
    
    @GetMapping("/category")
    public String category(Model m, @RequestParam(name = "pageNo",defaultValue = "0") Integer pageNo,
        @RequestParam(name = "pageSize",defaultValue= "9") Integer pageSize) {

        Page<Category> page = categoryService.getAllCategoryPagination(pageNo,pageSize);
        List<Category> categories = page.getContent();
		m.addAttribute("categories",categories);
		m.addAttribute("categoriesSize", categories.size());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());
        return "admin/category";
    }

    @PostMapping("/saveCategory")
    public String saveCategory(@ModelAttribute Category category, @RequestParam MultipartFile file, HttpSession session) throws IOException {

        String imageUrl = commonUtil.getImageUrl(file, BucketType.CATEGORY.getId());
        category.setImageName(imageUrl);

        if (categoryService.existCategory(category.getName())) {
            session.setAttribute("errorMsg", "Category Name already exists");
        } else if(file != null && !file.isEmpty()){
            Category savedCategory = categoryService.saveCategory(category);
            
            if (ObjectUtils.isEmpty(savedCategory)) {
                session.setAttribute("errorMsg", "Category not saved: Internal Server Error");
            } 
            else {
                fileService.uploadFileS3(file,BucketType.CATEGORY.getId());
                session.setAttribute("sucMsg", "Category saved successfully");
            }
        }
        return "redirect:/admin/category";
    }

    @GetMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session) {
        boolean isDeleted = categoryService.deleteCategory(id);

        if (isDeleted) {
            session.setAttribute("sucMsg", "Category deleted successfully");
        } else {
            session.setAttribute("errorMsg", "Something went wrong on server");
        }
        return "redirect:/admin/category";
    }

    @GetMapping("/loadEditCategory/{id}")
    public String loadEditCategory(@PathVariable("id") int id, Model model) {
        // Load the category from the database or service
        Category category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        return "admin/edit_category";
    }

    @PostMapping("/updateCategory")    
    public String updateCategory(@ModelAttribute Category category, @RequestParam("file") MultipartFile file,HttpSession session)
        throws IOException{

        Category oldCategory = categoryService.getCategoryById(category.getId());
        // String imageName =  file.isEmpty() ? oldCategory.getImageName() : file.getOriginalFilename();       
        
        if(!ObjectUtils.isEmpty(category)){
            oldCategory.setName(category.getName());
            oldCategory.setIsActive(category.getIsActive());
            if (!file.isEmpty()) {
                String imageUrl = commonUtil.getImageUrl(file, BucketType.CATEGORY.getId());
                oldCategory.setImageName(imageUrl);
            }        
        }

        Category updateCategory = categoryService.saveCategory(oldCategory);
        if(!ObjectUtils.isEmpty(updateCategory)){
            if(!file.isEmpty()){
                fileService.uploadFileS3(file,BucketType.CATEGORY.getId());
            }
            session.setAttribute("sucMsg", "Category updated Successfully");
        }
        else{
            session.setAttribute("errorMsg", "Something went wrong on server");
        }
        return "redirect:/admin/loadEditCategory/" + category.getId();
    }

    //Product Module

    @PostMapping("/saveProduct")
    public String saveProduct(@ModelAttribute Product product, 
                            @RequestParam("file") MultipartFile image, 
                            HttpSession session) throws IOException {

                                
        String imageUrl = commonUtil.getImageUrl(image, BucketType.PRODUCT.getId());
                                    
        // String imageName = image.isEmpty() ? "default.jpg" : image.getOriginalFilename();
        product.setImage(imageUrl);
        product.setDiscount(0);
        product.setDiscountPrice(product.getPrice());

        Product savedProduct = productService.saveProduct(product);

        if (!ObjectUtils.isEmpty(savedProduct)) {
            fileService.uploadFileS3(image,BucketType.PRODUCT.getId());
            session.setAttribute("sucMsg", "Product saved successfully!");
        } else {
            session.setAttribute("errorMsg", "Something went wrong on the server.");
        }
        return "redirect:/admin/loadAddProduct";
    }

    @GetMapping("/view_products")
    public String loadViewProduct(Model m, @RequestParam(value = "ch", required = false) String ch, HttpSession session,
    @RequestParam(name = "pageNo",defaultValue = "0") Integer pageNo,
    @RequestParam(name = "pageSize",defaultValue= "8") Integer pageSize){
        // if (ch != null && !ch.isEmpty()) {
        //     List<Product> searchList = productService.searchList(ch);
        //     if(ObjectUtils.isEmpty(searchList)){
        //         session.setAttribute("errorMsg","No product Found");
        //     }else{
        //         m.addAttribute("products", searchList);
        //     }
        // }else{
        //     m.addAttribute("products", productService.getAllProducts());
        // }
        
        Page<Product> searchList = null;
        if (ch != null && !ch.isEmpty()) {
            searchList = productService.searchListPagination(pageNo,pageSize,ch);
            if(ObjectUtils.isEmpty(searchList)){
                session.setAttribute("errorMsg","No product Found");
            }else{
                m.addAttribute("products", searchList);
            }
        }else{
            searchList = productService.getAllProductPagination(pageNo,pageSize);
            m.addAttribute("products", searchList);
        }

		m.addAttribute("products",searchList.getContent());
		m.addAttribute("pageNo", searchList.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", searchList.getTotalElements());
		m.addAttribute("totalPages", searchList.getTotalPages());
		m.addAttribute("isFirst", searchList.isFirst());
		m.addAttribute("isLast", searchList.isLast());

        return "admin/view_products";
    }

    @GetMapping("/deleteProduct/{id}")
    public String deleteProduct(@PathVariable int id, HttpSession session){
        Boolean deleteProduct = productService.deleteProduct(id);
        if(deleteProduct){
            session.setAttribute("sucMsg", "Product deleted");
        }
        else{
            session.setAttribute("errorMsg", "Something wrong on server");
        }
        return "redirect:/admin/view_products";
    }

    @GetMapping("/editProduct/{id}")
    public String editProduct(@PathVariable int id, Model m){
        m.addAttribute("product", productService.getProductById(id));
        m.addAttribute("categories", categoryService.getAllCategory());
        return "admin/edit_product";
    }

    @PostMapping("/updateProduct")
    public String updateProduct(@ModelAttribute Product product,
        @RequestParam("file") MultipartFile image,
        Model m, HttpSession session) {
        
        if(product.getDiscount() > 100 || product.getDiscount() < 0) {
            session.setAttribute("errorMsg", "Invalid Discount");
        }
        else{
            Product updateProduct = productService.updateProduct(product, image);
            if(!ObjectUtils.isEmpty(updateProduct)){
                session.setAttribute("sucMsg", "Product updated successfully");
            }else {
                session.setAttribute("errorMsg", "Error updating product");
            }
        }   

        return "redirect:/admin/editProduct/" + product.getId();     
    }

    @GetMapping("/users")
    public String getAllUsers(Model m,@RequestParam Integer type){
        List<UserDtls> users = null;
        if(type == 1){
            users = userService.getUsers("ROLE_USER");
        }else{
            users = userService.getUsers("ROLE_ADMIN");
        }
        m.addAttribute("userType", type);
        m.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/updateStatus")
    public String updateUserAccStatus(@RequestParam Boolean status, 
        @RequestParam Integer id,
        @RequestParam Integer type,
        HttpSession session){
        Boolean f = userService.updateAccStatus(id,status);

        if(f){
            session.setAttribute("sucMsg", "Account Status Updated");
        }else{
            session.setAttribute("errorMsg", "Something wrong on Server");
        }
        return "redirect:/admin/users?type=" + type;
    }

    @GetMapping("/orders")
    public String getAllOrders(Model m,@RequestParam(name = "pageNo",defaultValue = "0") Integer pageNo,
    @RequestParam(name = "pageSize",defaultValue= "6") Integer pageSize){
        // List<ProductOrder> orders = orderService.getAllOrders();
        // m.addAttribute("orders", orders);

        Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo,pageSize);
		m.addAttribute("orders",page.getContent());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

        return "admin/orders";
    }

    @PostMapping("/update-order-status")
    public String updateOrderStatus(@RequestParam Integer id, @RequestParam Integer st,HttpSession session){
        OrderStatus[] statusValues = OrderStatus.values();
        String status = null;

        for(OrderStatus stat : statusValues){
            if(stat.getId().equals(st)){
                status = stat.getName();
            }
        }
        ProductOrder updateOrder = orderService.updateOrderStatus(id,status);
        try {
            commonUtil.sendMailForProduct(updateOrder, status);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!ObjectUtils.isEmpty(updateOrder)){
            session.setAttribute("sucMsg", "Status Updated");
        } else {
            session.setAttribute("errorMsg", "Staus not Updated");
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/search-order")
    public String searchOrder(@RequestParam String orderId, Model m,HttpSession session,
    @RequestParam(name = "pageNo",defaultValue = "0") Integer pageNo,
    @RequestParam(name = "pageSize",defaultValue= "6") Integer pageSize){
        if(orderId == null || orderId.trim().isEmpty()){
            Page<ProductOrder> page = orderService.getAllOrdersPagination(pageNo,pageSize);
            m.addAttribute("orders",page.getContent());

            m.addAttribute("pageNo", page.getNumber());
            m.addAttribute("pageSize", pageSize);
            m.addAttribute("totalElements", page.getTotalElements());
            m.addAttribute("totalPages", page.getTotalPages());
            m.addAttribute("isFirst", page.isFirst());
            m.addAttribute("isLast", page.isLast());
        }else{
            ProductOrder ordersByOrderId = orderService.getOrdersByOrderId(orderId.trim());
            if(ObjectUtils.isEmpty(ordersByOrderId)){
                session.setAttribute("errorMsg", "Incorrect Order Id");
                return "redirect:/admin/orders";
            }else{
                List<ProductOrder> orderList = new ArrayList<>();
                orderList.add(ordersByOrderId);
                m.addAttribute("orders", orderList);
            }      
        }
        return "admin/orders";
    }

    @GetMapping("/add_admin")
    public String adminAdd(){
        return "admin/add_admin";
    }

    
	@PostMapping("/save-admin")
	public String saveAdmin(@ModelAttribute UserDtls user, 
		@RequestParam("img") MultipartFile file,
		HttpSession session) throws IOException {

		// String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
        String imageUrl = commonUtil.getImageUrl(file, BucketType.PROFILE.getId());
		user.setProfileImage(imageUrl);
		UserDtls saveUser = userService.saveAdmin(user);

		if (!ObjectUtils.isEmpty(saveUser)) {
			if (!file.isEmpty()) {
                fileService.uploadFileS3(file,BucketType.PROFILE.getId());
			}
			session.setAttribute("sucMsg", "Register successfully!");
		} else {
			session.setAttribute("errorMsg", "Something went wrong on the server.");
		}

		return "redirect:/admin/add_admin";
	}

    @GetMapping("/profile")
    public String profile() {
        return "admin/profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute UserDtls user, @RequestParam MultipartFile img, HttpSession session){
        UserDtls updateUserProfile = userService.updateUserProfile(user, img);

        if(ObjectUtils.isEmpty(updateUserProfile)){ 
            session.setAttribute("errorMsg", "Profile not Updated");
        }else{
            session.setAttribute("sucMsg", "Profile Updated Successfully!");
        }
        return "redirect:/admin/profile";
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
        return "redirect:/admin/profile";
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
