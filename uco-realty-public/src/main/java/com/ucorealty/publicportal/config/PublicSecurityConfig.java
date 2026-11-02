package com.ucorealty.publicportal.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PublicSecurityConfig {

  @Bean
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        .requestMatchers("/", "/projects", "/project/**", "/lead", "/files/**",
                         "/assets/**", "/css/**", "/images/**", "/js/**",
                         "/api/public/**", "/error", "/WEB-INF/jsp/**").permitAll()
        .anyRequest().denyAll()
      );

    return http.build();
  }
}
