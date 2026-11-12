package com.ucorealty.config;

import com.ucorealty.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Runs after a staff member presses Logout: writes a LOGOUT line to the audit trail and sends the browser to the login page.
 * Change this file when: something else must happen at logout (for example a redirect to the bank portal).
 */
@Component
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

  private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);

  private final AuditService auditService;

  public CustomLogoutSuccessHandler(AuditService auditService) {
    this.auditService = auditService;
    setDefaultTargetUrl("/login?logout=true");
  }

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                              Authentication authentication) throws IOException, ServletException {
    if (authentication != null && authentication.getName() != null) {
      auditService.logActivity(authentication.getName(), "LOGOUT", "AUTH", authentication.getName(), "User successfully logged out");
      log.info("[UCO SSO] {} signed out", authentication.getName());
    }
    super.onLogoutSuccess(request, response, authentication);
  }
}

