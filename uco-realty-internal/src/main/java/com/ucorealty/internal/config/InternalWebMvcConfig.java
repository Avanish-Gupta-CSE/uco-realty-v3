package com.ucorealty.internal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalWebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
        .addResourceLocations("/assets/", "classpath:/static/assets/");
    registry.addResourceHandler("/css/**")
        .addResourceLocations("/assets/css/", "classpath:/static/assets/css/");
    registry.addResourceHandler("/js/**")
        .addResourceLocations("/assets/js/", "classpath:/static/assets/js/");
    registry.addResourceHandler("/images/**")
        .addResourceLocations("/assets/images/", "classpath:/static/assets/images/");
  }
}

