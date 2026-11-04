package com.ucorealty.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import com.ucorealty.config.SecurityHeadersFilter;

@Configuration
public class PublicSecurityConfig {

  @Bean
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http
      // No staff session or login exists in this module; the only POST is the anonymous customer lead form.
      .csrf(AbstractHttpConfigurer::disable)
      .logout(AbstractHttpConfigurer::disable)   // no sessions or staff login exist here
      .headers(h -> h.disable())   // replaced by SecurityHeadersFilter (see config package)
      .addFilterBefore(new SecurityHeadersFilter(), DisableEncodeUrlFilter.class)
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
