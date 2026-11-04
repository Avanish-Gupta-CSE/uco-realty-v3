package com.ucorealty.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PublicWebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
        .addResourceLocations("/assets/", "classpath:/static/assets/")
        .setCachePeriod(3600);
    registry.addResourceHandler("/css/**")
        .addResourceLocations("/assets/css/", "classpath:/static/assets/css/")
        .setCachePeriod(3600);
    registry.addResourceHandler("/js/**")
        .addResourceLocations("/assets/js/", "classpath:/static/assets/js/")
        .setCachePeriod(3600);
    registry.addResourceHandler("/images/**")
        .addResourceLocations("/assets/images/", "classpath:/static/assets/images/")
        .setCachePeriod(3600);
  }
}
