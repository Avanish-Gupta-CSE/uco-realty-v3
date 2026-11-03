package com.ucorealty.security;

import com.ucorealty.hrms.HrmsService;
import com.ucorealty.hrms.HrmsUserProfile;
import com.ucorealty.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

  private final AuditService auditService;
  private final LoginAttemptLimiter loginLimiter;
  private final HrmsService hrmsService;

  public CustomAuthenticationSuccessHandler(AuditService auditService, LoginAttemptLimiter loginLimiter) {
    this(auditService, loginLimiter, null);
  }

  @Autowired
  public CustomAuthenticationSuccessHandler(AuditService auditService, LoginAttemptLimiter loginLimiter,
                                            @Autowired(required = false) @Lazy HrmsService hrmsService) {
    this.auditService = auditService;
    this.loginLimiter = loginLimiter;
    this.hrmsService = hrmsService;
    setDefaultTargetUrl("/admin/projects");
    setAlwaysUseDefaultTargetUrl(true);
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
    String username = authentication != null ? authentication.getName() : "UNKNOWN";
    String ip = extractClientIp(request);
    
    auditService.logLoginAttempt(username, ip, true, "Successful authentication");
    loginLimiter.recordSuccess(username);

    // Populate UCO Bank Single Sign-On session attributes
    HttpSession session = request.getSession(true);
    session.setAttribute("login_time", LocalDateTime.now());
    if (hrmsService != null) {
      HrmsUserProfile profile = hrmsService.fetchUserProfile(username);
      if (profile != null) {
        session.setAttribute("emp_id", profile.getEmpNo());
        session.setAttribute("sol_id", profile.getPostingSolId());
        session.setAttribute("UEMPNO", profile.getEmpNo());
        session.setAttribute("USOL", profile.getPostingSolId());
        session.setAttribute("USER_NAME", profile.getName());
        session.setAttribute("USER_ROLE", profile.getResolvedRole());
        session.setAttribute("USER_ZONE", profile.getResolvedZone());
        session.setAttribute("CADRE", profile.getCadre());
        session.setAttribute("SCALE", profile.getScaleCode());
      } else {
        session.setAttribute("emp_id", username);
        session.setAttribute("UEMPNO", username);
      }
    } else {
      session.setAttribute("emp_id", username);
      session.setAttribute("UEMPNO", username);
    }

    super.onAuthenticationSuccess(request, response, authentication);
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

