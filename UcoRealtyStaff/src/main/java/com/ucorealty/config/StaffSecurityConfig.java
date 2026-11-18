package com.ucorealty.config;

import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * Staff authentication happens only through bank SSO / test OTP (see HomeController), which builds the
 * Spring Security context itself. There is no form login, so unauthenticated requests to protected
 * pages are sent to /login (the SSO landing page).
 * Change this file when: a staff URL must be open without login, or needs a certain role (the role names are in util/StaffRoles).
 */
@Configuration
public class StaffSecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(StaffSecurityConfig.class);

  private final CustomLogoutSuccessHandler logoutSuccessHandler;

  public StaffSecurityConfig(CustomLogoutSuccessHandler logoutSuccessHandler) {
    this.logoutSuccessHandler = logoutSuccessHandler;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    // Spring Security answers 403 by itself (wrong role, or a form posted without a valid CSRF token); this writes it to the log first.
    AccessDeniedHandler standard = new AccessDeniedHandlerImpl();
    AccessDeniedHandler logged = (request, response, denied) -> {
      // A missing/old security token (CsrfException) is not a role problem: the page in the browser belongs to a session the server
      // no longer has (idle 30 minutes, restart, cached page). Send the user to the login page with the "session expired" notice.
      if (denied instanceof CsrfException) {
        log.info("[UCO SECURITY] Session expired or stale page: {} {} had no valid security token, sent to login ({})",
            request.getMethod(), request.getRequestURI(), denied.getMessage());
        response.sendRedirect(request.getContextPath() + "/login?expired");
        return;
      }
      log.warn("[UCO SECURITY] 403 {} {} by {} from {}: {}", request.getMethod(), request.getRequestURI(),
          request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName(), request.getRemoteAddr(), denied.getMessage());
      standard.handle(request, response, denied);
    };
    http
      // CSRF protection is on (Spring's default) for every staff form and the logout button.
      .headers(h -> h.disable())   // replaced by SecurityHeadersFilter (see config package)
      .addFilterBefore(new SecurityHeadersFilter(), DisableEncodeUrlFilter.class)
      .authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.INCLUDE).permitAll()   // INCLUDE: the shared header/footer of every JSP
        // Open pages: only the login steps and static files. Everything else, the project catalog and every file too, needs a login (secure coding order, point 3).
        .requestMatchers("/", "/home", "/entry",
                         "/login", "/sso/generate-otp", "/otp", "/otp-verify", "/error-page", "/error",
                         "/assets/**", "/css/**", "/images/**", "/js/**").permitAll()
        // Superadmin DIT audit logs strictly restricted:
        .requestMatchers("/admin/audit-logs/**").hasRole("SUPERADMIN_DIT")
        // All other internal features require authentication:
        .requestMatchers("/admin/**", "/api/admin/**").authenticated()
        .anyRequest().authenticated()
      )
      .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
        // an old session cookie the server no longer knows = the 30 idle minutes ran out: tell the user on the login page
        boolean expired = request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
        response.sendRedirect(request.getContextPath() + "/login" + (expired ? "?expired" : ""));
      }).accessDeniedHandler(logged))
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
