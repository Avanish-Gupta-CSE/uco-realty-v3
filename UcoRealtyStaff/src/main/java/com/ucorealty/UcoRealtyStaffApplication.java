package com.ucorealty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Starts the staff app (UcoRealtyStaff); Spring Boot finds every class under com.ucorealty from here.
 * No local users exist (staff come in through bank SSO), so the default in-memory user Spring Boot would otherwise generate is switched off.
 * Change this file when: the way the application starts changes (rarely).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan(basePackages = {"com.ucorealty.model"})
@EnableJpaRepositories(basePackages = {"com.ucorealty.repository"})
public class UcoRealtyStaffApplication extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(UcoRealtyStaffApplication.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(UcoRealtyStaffApplication.class, args);
  }
}

