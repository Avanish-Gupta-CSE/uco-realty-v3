package com.ucorealty.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import com.ucorealty.config.SecurityHeadersFilter;

/**
 * Staff authentication happens only through bank SSO / OTP (see SsoEntryController), which builds the
 * Spring Security context itself. There is no form login, so unauthenticated requests to protected
 * pages are sent to /login (the SSO landing page).
 */
@Configuration
public class InternalSecurityConfig {

  private final CustomLogoutSuccessHandler logoutSuccessHandler;

  public InternalSecurityConfig(CustomLogoutSuccessHandler logoutSuccessHandler) {
    this.logoutSuccessHandler = logoutSuccessHandler;
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http
      // CSRF is on for every staff form/logout. Only the anonymous customer lead capture is exempt.
      .csrf(csrf -> csrf.ignoringRequestMatchers("/lead"))
      .headers(h -> h.disable())   // replaced by SecurityHeadersFilter (see config package)
      .addFilterBefore(new SecurityHeadersFilter(), DisableEncodeUrlFilter.class)
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
      .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
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
