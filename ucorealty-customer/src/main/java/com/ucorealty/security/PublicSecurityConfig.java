package com.ucorealty.security;

import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import com.ucorealty.config.SecurityHeadersFilter;

@Configuration
public class PublicSecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(PublicSecurityConfig.class);

  @Bean
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    // Anything outside the public pages is denied; the log shows who is probing which URL.
    AccessDeniedHandler standard = new AccessDeniedHandlerImpl();
    AccessDeniedHandler logged = (request, response, denied) -> {
      log.warn("[UCO SECURITY] Denied {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
      standard.handle(request, response, denied);
    };
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
      )
      .exceptionHandling(ex -> ex.accessDeniedHandler(logged));

    return http.build();
  }
}
