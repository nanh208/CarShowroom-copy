package com.ares.shop.config;

import com.ares.shop.web.UploadController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry r) {
        try { java.nio.file.Files.createDirectories(UploadController.DIR); } catch (java.io.IOException ignored) {}
        String loc = UploadController.DIR.toUri().toString();
        r.addResourceHandler("/uploads/**").addResourceLocations(loc.endsWith("/") ? loc : loc + "/");
    }
}
