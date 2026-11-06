package com.ucorealty.config;

import com.ucorealty.interceptor.SessionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InternalWebMvcConfig implements WebMvcConfigurer {

  private final SessionInterceptor sessionInterceptor;

  public InternalWebMvcConfig(SessionInterceptor sessionInterceptor) {
    this.sessionInterceptor = sessionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(sessionInterceptor).addPathPatterns("/admin/**", "/api/admin/**");
  }

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

