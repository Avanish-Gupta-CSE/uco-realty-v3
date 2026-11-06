package com.ucorealty.security;

import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import com.ucorealty.config.SecurityHeadersFilter;

/**
 * Staff authentication happens only through bank SSO / test OTP (see HomeController), which builds the
 * Spring Security context itself. There is no form login, so unauthenticated requests to protected
 * pages are sent to /login (the SSO landing page).
 */
@Configuration
public class InternalSecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(InternalSecurityConfig.class);

  private final CustomLogoutSuccessHandler logoutSuccessHandler;

  public InternalSecurityConfig(CustomLogoutSuccessHandler logoutSuccessHandler) {
    this.logoutSuccessHandler = logoutSuccessHandler;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    // Spring Security answers 403 by itself (wrong role, or a form posted without a valid CSRF token); this writes it to the log first.
    AccessDeniedHandler standard = new AccessDeniedHandlerImpl();
    AccessDeniedHandler logged = (request, response, denied) -> {
      log.warn("[UCO SECURITY] 403 {} {} by {} from {}: {}", request.getMethod(), request.getRequestURI(),
          request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName(), request.getRemoteAddr(), denied.getMessage());
      standard.handle(request, response, denied);
    };
    http
      // CSRF is on for every staff form/logout. Only the anonymous customer lead capture is exempt.
      .csrf(csrf -> csrf.ignoringRequestMatchers("/lead"))
      .headers(h -> h.disable())   // replaced by SecurityHeadersFilter (see config package)
      .addFilterBefore(new SecurityHeadersFilter(), DisableEncodeUrlFilter.class)
      .authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        // Open pages: the catalog, the login steps, static files. Everything else needs a login.
        .requestMatchers("/", "/home", "/entry", "/projects", "/project/**", "/lead", "/files/**",
                         "/login", "/sso/generate-otp", "/otp", "/otp-verify", "/error-page", "/error",
                         "/assets/**", "/css/**", "/images/**", "/js/**").permitAll()
        // Superadmin DIT audit logs strictly restricted:
        .requestMatchers("/admin/audit-logs/**").hasRole("SUPERADMIN_DIT")
        // All other internal features require authentication:
        .requestMatchers("/admin/**", "/api/admin/**").authenticated()
        .anyRequest().authenticated()
      )
      .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")).accessDeniedHandler(logged))
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
