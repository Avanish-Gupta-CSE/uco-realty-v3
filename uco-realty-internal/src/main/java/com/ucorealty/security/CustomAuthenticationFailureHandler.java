package com.ucorealty.security;

import com.ucorealty.service.AuditService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final AuditService auditService;
  private final LoginAttemptLimiter loginLimiter;

  public CustomAuthenticationFailureHandler(AuditService auditService, LoginAttemptLimiter loginLimiter) {
    this.auditService = auditService;
    this.loginLimiter = loginLimiter;
    setDefaultFailureUrl("/login?error=true");
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
    String username = request.getParameter("username");
    if (username == null || username.trim().isEmpty()) {
      username = "UNKNOWN";
    }
    String ip = extractClientIp(request);
    String remarks = exception != null && exception.getMessage() != null ? exception.getMessage() : "Invalid credentials";

    auditService.logLoginAttempt(username, ip, false, remarks);
    loginLimiter.recordFailure(username);

    super.onAuthenticationFailure(request, response, exception);
  }

  private String extractClientIp(HttpServletRequest request) {
    String xf = request.getHeader("X-Forwarded-For");
    if (xf != null && !xf.trim().isEmpty() && !"unknown".equalsIgnoreCase(xf.trim())) {
      return xf.split(",")[0].trim();
    }
    String remoteAddr = request.getRemoteAddr();
    return remoteAddr != null ? remoteAddr : "UNKNOWN";
  }
}

