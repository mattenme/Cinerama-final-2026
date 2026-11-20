package pe.edu.upeu.cinerama.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cinerama.upload.dir:uploads}")
    private String uploadDir;

    private final AutorizacionInterceptor autorizacionInterceptor;

    public WebConfig(AutorizacionInterceptor autorizacionInterceptor) {
        this.autorizacionInterceptor = autorizacionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacionInterceptor).addPathPatterns("/api/v1/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadLocation = "file:" + Paths.get(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
