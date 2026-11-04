package com.ucorealty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// No local users exist: skip the default in-memory user Spring Boot would otherwise generate.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
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

