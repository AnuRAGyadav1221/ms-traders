

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = "file:///" + uploadDir.replace(" ", "%20");

        registry.addResourceHandler("/product_img/**")
            .addResourceLocations(path + "product_img/");

        registry.addResourceHandler("/category_img/**")
                .addResourceLocations(path + "category_img/");

        registry.addResourceHandler("/banner_img/**")
                .addResourceLocations(path + "banner_img/");

        registry.addResourceHandler("/profile_img/**")
                .addResourceLocations(path + "profile_img/");
    
    }
}
