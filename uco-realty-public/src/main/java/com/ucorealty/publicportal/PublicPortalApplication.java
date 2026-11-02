package com.ucorealty.publicportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ucorealty.publicportal",
    "com.ucorealty.service"
})
@EntityScan(basePackages = "com.ucorealty.entity")
@EnableJpaRepositories(basePackages = "com.ucorealty.repository")
public class PublicPortalApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(PublicPortalApplication.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(PublicPortalApplication.class, args);
  }
}

