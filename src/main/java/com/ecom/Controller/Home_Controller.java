package com.ecom.Controller;

// import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.model.UserDtls;
import com.ecom.service.CartService;
import com.ecom.service.CategoryService;
import com.ecom.service.FileService;
import com.ecom.service.ProductService;
import com.ecom.service.UserService;
import com.ecom.util.BucketType;
import com.ecom.util.CommonUtil;

import io.micrometer.common.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class Home_Controller {

	@Autowired
	private FileService fileService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private UserService userService;

	@Autowired
	private CartService cartService;

	@Autowired
	private BCryptPasswordEncoder encoder;


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
	public String index(Model m) {

		List<Category> allActiveCategories = categoryService.getAllActiveCategories().stream()
			.sorted((c1, c2) -> Integer.compare(c2.getId(), c1.getId())).limit(6).toList();

		List<Product> allActiveProducts = productService.getAllActiveProducts("").stream()
			.sorted((p1, p2) -> Integer.compare(p2.getId(), p1.getId()))
			.limit(8)
			.toList();


		m.addAttribute("products", allActiveProducts);
		m.addAttribute("categories", allActiveCategories);
		return "index";
	}
	
	@GetMapping("/signin")
	public String login() {
		return "login";
	}
	
	@GetMapping("/register")
	public String register() {
		return "register";
	}
	
	@GetMapping("/products")
	public String product(Model m,@RequestParam(value = "category",defaultValue = "") String category,
		@RequestParam(name = "pageNo",defaultValue = "0") Integer pageNo,
		@RequestParam(name = "pageSize",defaultValue= "8") Integer pageSize,
		@RequestParam(defaultValue = "") String ch) {

		List<Category> categories = categoryService.getAllActiveCategories();
		m.addAttribute("categories",categories);

		Page<Product> page = null;
		if(StringUtils.isEmpty(ch)){
			page = productService.getAllActiveProductPagination(pageNo,pageSize,category);
		}else{
			page = productService.searchActiveProductPagination(pageNo,pageSize,category,ch);
		}

		List<Product> products = page.getContent();
		m.addAttribute("products",products);
		m.addAttribute("productsSize", products.size());

		m.addAttribute("pageNo", page.getNumber());
		m.addAttribute("pageSize", pageSize);
		m.addAttribute("totalElements", page.getTotalElements());
		m.addAttribute("totalPages", page.getTotalPages());
		m.addAttribute("isFirst", page.isFirst());
		m.addAttribute("isLast", page.isLast());

		return "product";
	}
	
	@GetMapping("/product_view/{id}")
	public String view(@PathVariable int id, Model m, Principal p) {
		Product productById = productService.getProductById(id);
		m.addAttribute("product",productById);
		return "view";
	}

	@PostMapping("/saveUser")
	public String saveUser(@ModelAttribute UserDtls user, 
		@RequestParam("img") MultipartFile file,
		HttpSession session) throws IOException {

		Boolean existsEmail = userService.existsEmail(user.getEmail());
		if(existsEmail){
			session.setAttribute("errorMsg", "Email already exist...");
		}else{
			
			// String imageName = file.isEmpty() ? "default.jpg" : file.getOriginalFilename();
			String imageUrl = commonUtil.getImageUrl(file, BucketType.PROFILE.getId());

			user.setProfileImage(imageUrl);
			UserDtls saveUser = userService.saveUser(user);
			
			if (!ObjectUtils.isEmpty(saveUser)) {
				if (!file.isEmpty()) {
					// File saveFile = new ClassPathResource("static/images").getFile();

					// File profileImgDir = new File(saveFile.getAbsolutePath() + File.separator + "profile_img");
					// if (!profileImgDir.exists()) {
					// 	profileImgDir.mkdirs();
					// }

					// Path path = Paths.get(profileImgDir.getAbsolutePath(), imageName);
					// Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

					fileService.uploadFileS3(file,BucketType.PROFILE.getId());

				}

				session.setAttribute("sucMsg", "Register successfully!");
			} else {
				session.setAttribute("errorMsg", "Something went wrong on the server.");
			}
		}

		return "redirect:/register";
	}

	@GetMapping("/forgotPassword")
	public String forgotPasswordPage(){
		return "forgot_pass";
	}

	@PostMapping("/forgotPassword") //Use RedirectAttributes instead of HttpSession for temporary messages.
  
	public String processForgotPasswordPage(@RequestParam String email, 
	RedirectAttributes redirectAttributes, 
	HttpServletRequest request) throws UnsupportedEncodingException, MessagingException{
		UserDtls userByEmail = userService.getUserByEmail(email);

		if(ObjectUtils.isEmpty(userByEmail)){
			redirectAttributes.addFlashAttribute("errorMsg", "Invalid Email");
		}else{

			String resetToken = UUID.randomUUID().toString();
			userService.updateUserResetToken(email, resetToken);

			//Generate URL : Sample -> "http://localhost:8080/resetPassword?token=asddsdujbfdd"

			String genereatedURL = CommonUtil.generateURL(request) + "/resetPassword?token=" + resetToken;

			Boolean mailSend = commonUtil.sendMail(genereatedURL,email);
			
			if(mailSend){
				redirectAttributes.addFlashAttribute("sucMsg", "Please cheack your email...! Password Reset link send!!!");
			}else{
				redirectAttributes.addFlashAttribute("errorMsg", "Something wrong on server...! Email not send");

			}
		}
		return "redirect:/forgotPassword";
	}
	
	@GetMapping("/resetPassword")
	public String resetPasswordPage(@RequestParam String token, 
	HttpSession session, Model m){
		UserDtls userByToken = userService.getUserByToken(token);
		if(ObjectUtils.isEmpty(userByToken)){
			m.addAttribute("Msg", "User link is Invalid or Expired");
			return "messagePage";
		}
		m.addAttribute("token", token);
		return "reset_password";
	}

	@PostMapping("/resetPassword")
	public String confirmPasswordPage(@RequestParam String token,@RequestParam String password,
	HttpSession session, Model m){
		UserDtls userByToken = userService.getUserByToken(token);
		if(ObjectUtils.isEmpty(userByToken)){
			m.addAttribute("Msg", "User link is Invalid or Expired");
			return "messagePage";
		}else{
			userByToken.setPassword(encoder.encode(password));
			userByToken.setResetToken(null);
			userService.updateUser(userByToken);
			m.addAttribute("Msg", "Password change successfully");
			return "messagePage";
		}
		
	}

	@GetMapping("/search")
    public String searchProduct(@RequestParam String ch, Model m){
		List<Product> searchList = productService.searchList(ch);
		List<Category> categories = categoryService.getAllActiveCategories();
		m.addAttribute("categories",categories);
		m.addAttribute("products",searchList);
        return "product";
    }
}
