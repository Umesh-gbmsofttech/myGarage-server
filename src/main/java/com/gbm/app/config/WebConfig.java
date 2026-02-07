package com.gbm.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String profileUploadDir;

    public WebConfig(@Value("${app.upload.dir}") String uploadDir,
            @Value("${app.profile.upload.dir}") String profileUploadDir) {
        this.uploadDir = uploadDir;
        this.profileUploadDir = profileUploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize().toString() + "/";
        registry.addResourceHandler("/uploads/banners/**")
            .addResourceLocations(location);

        String profileLocation = "file:" + java.nio.file.Paths.get(profileUploadDir).toAbsolutePath().normalize().toString() + "/";
        registry.addResourceHandler("/uploads/profiles/**")
            .addResourceLocations(profileLocation);
    }
}
