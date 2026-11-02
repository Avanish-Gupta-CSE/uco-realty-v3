package com.ucorealty.internal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ucorealty.internal",
    "com.ucorealty.service",
    "com.ucorealty.hrms",
    "com.ucorealty.util",
    "com.ucorealty.repository"
})
@EntityScan(basePackages = {"com.ucorealty.entity"})
@EnableJpaRepositories(basePackages = {"com.ucorealty.repository"})
public class InternalPortalApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(InternalPortalApplication.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(InternalPortalApplication.class, args);
  }
}

