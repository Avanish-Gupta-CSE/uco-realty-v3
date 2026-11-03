package com.ucorealty.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class InternalSecurityConfig {

  private final AppUserDetailsService userDetailsService;
  private final CustomAuthenticationSuccessHandler successHandler;
  private final CustomAuthenticationFailureHandler failureHandler;
  private final CustomLogoutSuccessHandler logoutSuccessHandler;

  public InternalSecurityConfig(AppUserDetailsService userDetailsService,
                                CustomAuthenticationSuccessHandler successHandler,
                                CustomAuthenticationFailureHandler failureHandler,
                                CustomLogoutSuccessHandler logoutSuccessHandler) {
    this.userDetailsService = userDetailsService;
    this.successHandler = successHandler;
    this.failureHandler = failureHandler;
    this.logoutSuccessHandler = logoutSuccessHandler;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .userDetailsService(userDetailsService)
      .authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        // Full intranet access to catalog, bank production SSO entry points, OTP, static assets, JSP views, and error page:
        .requestMatchers("/", "/projects", "/project/**", "/lead", "/files/**",
                         "/entry", "/GetValue.jsp", "/GetValue", "/getvalue", "/getvalue.jsp",
                         "/sso/**", "/login", "/otp", "/otp-verify", "/error-page", "/error",
                         "/assets/**", "/css/**", "/images/**", "/js/**", "/WEB-INF/jsp/**").permitAll()
        // Superadmin DIT audit logs strictly restricted:
        .requestMatchers("/admin/audit-logs/**").hasRole("SUPERADMIN_DIT")
        // All other internal features require authentication:
        .requestMatchers("/admin/**", "/api/admin/**").authenticated()
        .anyRequest().authenticated()
      )
      .formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/login")
        .successHandler(successHandler)
        .failureHandler(failureHandler)
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessHandler(logoutSuccessHandler)
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID")
        .permitAll()
      );

    return http.build();
  }
}
