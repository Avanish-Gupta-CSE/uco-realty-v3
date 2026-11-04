package com.ucorealty.controller;

import com.ucorealty.entity.AppUser;
import com.ucorealty.security.StaffPrincipal;
import com.ucorealty.hrms.HrmsService;
import com.ucorealty.hrms.HrmsUserProfile;
import com.ucorealty.service.AuditService;
import com.ucorealty.util.SsoCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * UCO Bank Corporate Intranet Single Sign-On (SSO) Entry Controller.
 * Implements bank production AES-GCM encrypted handoff (/entry?value=...)
 * alongside OTP verification and direct intranet redirection.
 */
@Controller
public class SsoEntryController {

  private static final Logger logger = LoggerFactory.getLogger(SsoEntryController.class);

  // No defaults on purpose: the application must refuse to start without the bank's SSO secrets.
  @Value("${sso.encryption.key}")
  private String encryptionKey;

  @Value("${sso.encryption.iv:}")
  private String encryptionIv;

  @Value("${uco.hrms.mode:bank}")
  private String hrmsMode;

  // Local/UAT-only conveniences (plain /sso handoff, app-issued OTP, test-user picker). Always false in prod.
  @Value("${uco.sso.dev-login-enabled:false}")
  private boolean devLoginEnabled;

  @Value("${app.redirect.url:/admin/projects}")
  private String redirectUrl;

  private final HrmsService hrmsService;
  private final AuditService auditService;

  public SsoEntryController(HrmsService hrmsService, AuditService auditService) {
    this.hrmsService = hrmsService;
    this.auditService = auditService;
  }

  @GetMapping("/")
  public String rootRedirect(Authentication auth) {
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
      return "redirect:/admin/projects";
    }
    return "redirect:/projects";
  }

  /**
   * Bank Production Single Sign-On Entry Point.
   * Parameter 'value' or 'Value' contains Base64-encoded encrypted token.
   * Directly supports GetValue.jsp specification (AES/CBC/PKCS5Padding) from db-login-reference
   * alongside AES-GCM encrypted tokens.
   */
  @GetMapping({"/entry", "/GetValue.jsp", "/GetValue", "/getvalue", "/getvalue.jsp", "/sso/entry"})
  public String entryPoint(@RequestParam(value = "value", required = false) String value,
                           @RequestParam(value = "Value", required = false) String capitalValue,
                           @RequestParam(value = "token", required = false) String token,
                           HttpServletRequest request,
                           Model model) {
    String rawToken = (value != null && !value.trim().isEmpty()) ? value :
                      ((capitalValue != null && !capitalValue.trim().isEmpty()) ? capitalValue : token);

    if (rawToken == null || rawToken.trim().isEmpty()) {
      HttpSession session = request.getSession(false);
      if (session != null && (session.getAttribute("emp_id") != null)) {
        return "redirect:" + redirectUrl;
      }
      logger.info("[UCO SSO /entry] Access attempt without token. Redirecting to staff login.");
      return "redirect:/login";
    }

    try {
      String normalized = rawToken.replaceAll("\\s+", "+");
      String decrypted = SsoCryptoUtil.decryptPayload(normalized, encryptionKey, encryptionIv);

      SsoCryptoUtil.SsoAuthInfo authInfo = SsoCryptoUtil.parseAuthInfo(decrypted);
      if (authInfo == null || authInfo.getEmpNo() == null || authInfo.getEmpNo().trim().isEmpty()) {
        logger.warn("[UCO SSO /entry] Decrypted payload missing employee identifier");
        auditService.logLoginAttempt("UNKNOWN", extractClientIp(request), false, "SSO token decrypted but has no employee id");
        return "redirect:/login?error=invalid_token";
      }

      return authenticateAndEstablishSession(authInfo.getEmpNo().trim(), authInfo.getSolId(),
          request, "SSO Encrypted Payload (/entry / GetValue.jsp)");

    } catch (Exception e) {
      logger.error("[UCO SSO /entry] Decryption failed: {}", e.getMessage());
      auditService.logLoginAttempt("UNKNOWN", extractClientIp(request), false, "SSO token could not be decrypted");
      return "redirect:/login?error=sso_failed";
    }
  }

  /**
   * Intranet Portal Forwarding Endpoint (/sso).
   * Supports encrypted 'value'/'Value' token and query parameter fallback (?emp_id=...&sol_id=... or ?empno=...&sol=...).
   */
  @GetMapping("/sso")
  public String portalRedirectSso(@RequestParam(value = "value", required = false) String value,
                                  @RequestParam(value = "Value", required = false) String capitalValue,
                                  @RequestParam(value = "token", required = false) String token,
                                  @RequestParam(value = "emp_id", required = false) String empId,
                                  @RequestParam(value = "empno", required = false) String empno,
                                  @RequestParam(value = "sol_id", required = false) String solId,
                                  @RequestParam(value = "sol", required = false) String sol,
                                  HttpServletRequest request) {
    if (value != null && !value.trim().isEmpty()) {
      return entryPoint(value, null, null, request, null);
    }
    if (capitalValue != null && !capitalValue.trim().isEmpty()) {
      return entryPoint(null, capitalValue, null, request, null);
    }
    if (token != null && !token.trim().isEmpty()) {
      return entryPoint(null, null, token, request, null);
    }

    // A plain ?emp_id= URL carries no proof of identity. It exists for local/UAT testing only.
    if (!devLoginEnabled) {
      logger.warn("[UCO SSO /sso] Plain employee-id handoff rejected (dev login disabled)");
      auditService.logLoginAttempt("UNKNOWN", extractClientIp(request), false, "Plain /sso handoff rejected in this environment");
      return "redirect:/login?error=sso_failed";
    }

    String effectiveEmpId = (empId != null && !empId.trim().isEmpty()) ? empId : empno;
    String effectiveSolId = (solId != null && !solId.trim().isEmpty()) ? solId : sol;

    if (effectiveEmpId == null || effectiveEmpId.trim().isEmpty()) {
      return "redirect:/login";
    }

    return authenticateAndEstablishSession(effectiveEmpId.trim(), effectiveSolId, request, "Direct Portal SSO handoff (/sso)");
  }

  /**
   * Core helper to authenticate employee against HRMS, build the signed-in user, and set the session attributes.
   */
  private String authenticateAndEstablishSession(String empId, String solId,
                                                HttpServletRequest request, String loginMechanism) {
    HrmsUserProfile profile = hrmsService.fetchUserProfile(empId);
    if (profile == null || !profile.isActive()) {
      logger.warn("[UCO SSO] Employee record {} could not be verified in HRMS.", empId);
      auditService.logLoginAttempt(empId, extractClientIp(request), false, loginMechanism + " - employee not found/inactive in HRMS");
      return "redirect:/error-page?reason=invalid_employee";
    }

    // Role and zone come from the HRMS posting, never from the URL. Only local/UAT test handoffs may override the SOL.
    if (devLoginEnabled && solId != null && !solId.trim().isEmpty()) {
      profile.setPostingSolId(solId.trim());
    }

    if (hrmsService.resolveRole(profile) == null) {
      logger.warn("[UCO SSO] Employee {} (SOL {}) has no role in this portal.", profile.getEmpNo(), profile.getPostingSolId());
      auditService.logLoginAttempt(empId, extractClientIp(request), false,
          loginMechanism + " - not authorised for this portal (SOL " + profile.getPostingSolId() + ")");
      return "redirect:/error-page?reason=not_authorized";
    }

    AppUser appUser = hrmsService.toAppUser(profile);
    HttpSession session = freshSession(request);

    UserDetails userDetails = new StaffPrincipal(appUser);
    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    populateBankSessionAttributes(session, profile);

    String ip = extractClientIp(request);
    auditService.logLoginAttempt(appUser.username, ip, true,
        loginMechanism + " - SOL: " + profile.getPostingSolId() + ", Role: " + appUser.role);

    logger.info("[UCO SSO] Successfully established intranet session for empId={}, solId={}, role={}",
        profile.getEmpNo(), profile.getPostingSolId(), appUser.role);

    return "redirect:/admin/projects";
  }

  /**
   * SSO Login Landing Page for direct login and OTP entry.
   */
  @GetMapping("/login")
  public String loginPage(Authentication auth, Model model) {
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
      return "redirect:/admin/projects";
    }
    model.addAttribute("devLogin", devLoginEnabled);
    model.addAttribute("localMode", "local".equalsIgnoreCase(hrmsMode));   // built-in test employees (laptop only)
    return "login";
  }

  /**
   * Generates OTP for valid UCO Bank Employee.
   */
  @PostMapping("/sso/generate-otp")
  public String generateOtp(@RequestParam String empId,
                            HttpServletRequest request) {
    if (!devLoginEnabled) {
      return "redirect:/login?error=sso_failed";
    }
    if (empId == null || empId.trim().isEmpty()) {
      return "redirect:/login?error=invalid_employee";
    }

    String normalized = empId.trim();
    HrmsUserProfile profile = hrmsService.fetchUserProfile(normalized);
    if (profile == null || !profile.isActive()) {
      logger.warn("[UCO SSO] OTP requested for non-existent or inactive employee: {}", normalized);
      return "redirect:/login?error=invalid_employee";
    }

    String otp = hrmsService.generateOtp(profile.getEmpNo());
    HttpSession session = request.getSession(true);
    session.setAttribute("PENDING_OTP_EMP_ID", profile.getEmpNo());
    session.setAttribute("PENDING_OTP_NAME", profile.getName());
    session.setAttribute("PENDING_OTP_SOL", profile.getPostingSolId() + " - " + profile.getPostingSolDesc());
    session.setAttribute("PENDING_OTP_MOBILE", maskMobile(profile.getMobile()));
    session.setAttribute("PENDING_OTP_ROLE", hrmsService.resolveRole(profile));

    logger.info("[UCO SSO] Generated OTP for employee {} ({})", profile.getEmpNo(), profile.getName());
    return "redirect:/otp";
  }

  /**
   * Renders the 6-digit OTP verification screen.
   */
  @GetMapping("/otp")
  public String otpPage(HttpServletRequest request, Model model) {
    HttpSession session = request.getSession(false);
    if (!devLoginEnabled || session == null || session.getAttribute("PENDING_OTP_EMP_ID") == null) {
      return "redirect:/login";
    }

    model.addAttribute("empId", session.getAttribute("PENDING_OTP_EMP_ID"));
    model.addAttribute("empName", session.getAttribute("PENDING_OTP_NAME"));
    model.addAttribute("empSol", session.getAttribute("PENDING_OTP_SOL"));
    model.addAttribute("empMobile", session.getAttribute("PENDING_OTP_MOBILE"));
    model.addAttribute("empRole", session.getAttribute("PENDING_OTP_ROLE"));
    return "otp";
  }

  /**
   * Verifies OTP and establishes authenticated Spring Security session with UCO Bank attributes.
   */
  @PostMapping("/otp-verify")
  public String verifyOtp(@RequestParam String otp,
                          HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (!devLoginEnabled || session == null || session.getAttribute("PENDING_OTP_EMP_ID") == null) {
      return "redirect:/login";
    }

    String empId = (String) session.getAttribute("PENDING_OTP_EMP_ID");
    boolean valid = hrmsService.verifyOtp(empId, otp);
    if (!valid) {
      logger.warn("[UCO SSO] Failed OTP verification attempt for employee {}", empId);
      return "redirect:/otp?error=otp";
    }

    HrmsUserProfile profile = hrmsService.fetchUserProfile(empId);
    if (profile == null || hrmsService.resolveRole(profile) == null) {
      return "redirect:/login?error=invalid_employee";
    }

    AppUser appUser = hrmsService.toAppUser(profile);
    session = freshSession(request);

    UserDetails userDetails = new StaffPrincipal(appUser);
    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    populateBankSessionAttributes(session, profile);

    String ip = extractClientIp(request);
    auditService.logLoginAttempt(appUser.username, ip, true,
        "SSO OTP Login - Cadre: " + profile.getCadre() + ", Role: " + appUser.role);

    session.removeAttribute("PENDING_OTP_EMP_ID");
    session.removeAttribute("PENDING_OTP_NAME");
    session.removeAttribute("PENDING_OTP_SOL");
    session.removeAttribute("PENDING_OTP_MOBILE");
    session.removeAttribute("PENDING_OTP_ROLE");

    logger.info("[UCO SSO] Successfully authenticated employee {} ({}) with role {}",
        profile.getEmpNo(), profile.getName(), appUser.role);

    return "redirect:/admin/projects";
  }

  /**
   * Custom Access Denied / Security Authentication Error Page.
   */
  @GetMapping("/error-page")
  public String errorPage(@RequestParam(value = "reason", required = false) String reason, Model model) {
    logger.warn("[UCO Portal] Rendering security error page with reason code: {}", reason);
    String errorMessage = "Access Denied: Missing Single Sign-On parameters. Please access the application through the authorized UCO Bank portal.";
    if ("unauthorized".equals(reason)) {
      errorMessage = "Security Authentication Failure: Invalid or unauthorized SSO token. Direct URL browsing is strictly prohibited.";
    } else if ("not_authorized".equals(reason)) {
      errorMessage = "Access Denied: Your posting (SOL) does not have access to the Realty portal. Please contact DIT SWDC Cell if you need access.";
    } else if ("invalid_employee".equals(reason)) {
      errorMessage = "Employee Authorization Failure: Your employee record could not be verified in the bank HRMS database. Please contact DIT SWDC Cell.";
    }
    model.addAttribute("errorMessage", errorMessage);
    return "error";
  }

  /** The SSO gives us only the employee number and SOL; role and zone come from HRMS and travel in the principal. */
  private void populateBankSessionAttributes(HttpSession session, HrmsUserProfile profile) {
    session.setAttribute("emp_id", profile.getEmpNo());
    session.setAttribute("sol_id", profile.getPostingSolId());
  }

  /**
   * New session id for every login (prevents session fixation); pre-login attributes such as the
   * pending OTP employee are carried over by changeSessionId().
   */
  private HttpSession freshSession(HttpServletRequest request) {
    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    return request.getSession(true);
  }

  private String maskMobile(String mobile) {
    if (mobile == null || mobile.length() < 4) {
      return "******";
    }
    return "******" + mobile.substring(mobile.length() - 4);
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

