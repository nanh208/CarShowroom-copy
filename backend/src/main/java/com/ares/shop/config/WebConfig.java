package com.ares.shop.config;

import com.ares.shop.web.UploadController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StaffPermissionInterceptor staffPerms;
    public WebConfig(StaffPermissionInterceptor staffPerms) { this.staffPerms = staffPerms; }
    @Override public void addInterceptors(InterceptorRegistry r) { r.addInterceptor(staffPerms).addPathPatterns("/api/admin/**"); }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry r) {
        try { java.nio.file.Files.createDirectories(UploadController.DIR); } catch (java.io.IOException ignored) {}
        String loc = UploadController.DIR.toUri().toString();
        r.addResourceHandler("/uploads/**").addResourceLocations(loc.endsWith("/") ? loc : loc + "/");
    }
}
