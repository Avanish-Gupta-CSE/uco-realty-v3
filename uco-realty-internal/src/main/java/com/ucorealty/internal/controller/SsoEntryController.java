package com.ucorealty.internal.controller;

import com.ucorealty.entity.AppUser;
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
import java.util.List;

/**
 * UCO Bank Corporate Intranet Single Sign-On (SSO) Entry Controller.
 * Implements bank production AES-GCM encrypted handoff (/entry?value=...)
 * alongside OTP verification and direct intranet redirection.
 */
@Controller
public class SsoEntryController {

  private static final Logger logger = LoggerFactory.getLogger(SsoEntryController.class);

  @Value("${getValue.key:UCOBANKSSOKEY2026!SECRET#128BITS}")
  private String encryptionKey;

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
   * Parameter 'value' contains Base64-encoded AES-GCM encrypted ciphertext with IV prefix.
   * Aligned with bank production reference project BranchRecordMaintainance (HomeController.java).
   */
  @GetMapping("/entry")
  public String entryPoint(@RequestParam(value = "value", required = false) String value,
                           HttpServletRequest request,
                           Model model) {
    if (value == null || value.trim().isEmpty()) {
      HttpSession session = request.getSession(false);
      if (session != null && session.getAttribute("emp_id") != null) {
        return "redirect:/admin/projects";
      }
      logger.info("[UCO SSO /entry] Access attempt without token. Redirecting to staff login.");
      return "redirect:/login";
    }

    try {
      String normalized = value.replaceAll("\\s+", "+");
      String decrypted = SsoCryptoUtil.decryptPayload(normalized, encryptionKey);
      logger.info("[UCO SSO /entry] Successfully decrypted SSO token payload: {}", decrypted);

      SsoCryptoUtil.SsoAuthInfo authInfo = SsoCryptoUtil.parseAuthInfo(decrypted);
      if (authInfo == null || authInfo.getEmpNo() == null || authInfo.getEmpNo().trim().isEmpty()) {
        logger.warn("[UCO SSO /entry] Decrypted payload missing employee identifier: {}", decrypted);
        return "redirect:/login?error=invalid_token";
      }

      return authenticateAndEstablishSession(authInfo.getEmpNo().trim(), authInfo.getSolId(),
          request, "SSO AES-GCM Encrypted Payload (/entry)");

    } catch (Exception e) {
      logger.error("[UCO SSO /entry] Decryption failed: {}", e.getMessage());
      return "redirect:/login?error=sso_failed";
    }
  }

  /**
   * Intranet Portal Forwarding Endpoint (/sso).
   * Supports both encrypted 'value' parameter and query parameter fallback (?emp_id=...&sol_id=...).
   */
  @GetMapping("/sso")
  public String portalRedirectSso(@RequestParam(value = "value", required = false) String value,
                                  @RequestParam(value = "emp_id", required = false) String empId,
                                  @RequestParam(value = "sol_id", required = false) String solId,
                                  HttpServletRequest request) {
    if (value != null && !value.trim().isEmpty()) {
      return entryPoint(value, request, null);
    }

    if (empId == null || empId.trim().isEmpty()) {
      return "redirect:/login";
    }

    return authenticateAndEstablishSession(empId.trim(), solId, request, "Direct Portal SSO handoff (/sso)");
  }

  /**
   * Core helper to authenticate employee against HRMS, sync AppUser, and set bank session attributes.
   */
  private String authenticateAndEstablishSession(String empId, String solId,
                                                HttpServletRequest request, String loginMechanism) {
    HrmsUserProfile profile = hrmsService.fetchUserProfile(empId);
    if (profile == null || !profile.isActive()) {
      logger.warn("[UCO SSO] Employee record {} could not be verified in HRMS.", empId);
      return "redirect:/error-page?reason=invalid_employee";
    }

    if (solId != null && !solId.trim().isEmpty()) {
      profile.setPostingSolId(solId.trim());
    }

    AppUser appUser = hrmsService.syncAppUser(profile);
    HttpSession session = request.getSession(true);

    UserDetails userDetails = new User(
        appUser.username,
        "",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.role))
    );
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
    List<HrmsUserProfile> sampleUsers = hrmsService.listAllDirectoryEmployees();
    model.addAttribute("sampleUsers", sampleUsers);
    return "login";
  }

  /**
   * Generates OTP for valid UCO Bank Employee.
   */
  @PostMapping("/sso/generate-otp")
  public String generateOtp(@RequestParam String empId,
                            HttpServletRequest request) {
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
    session.setAttribute("PENDING_OTP_ROLE", profile.getResolvedRole());

    logger.info("[UCO SSO] Generated OTP for employee {} ({})", profile.getEmpNo(), profile.getName());
    return "redirect:/otp";
  }

  /**
   * Renders the 6-digit OTP verification screen.
   */
  @GetMapping("/otp")
  public String otpPage(HttpServletRequest request, Model model) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("PENDING_OTP_EMP_ID") == null) {
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
    if (session == null || session.getAttribute("PENDING_OTP_EMP_ID") == null) {
      return "redirect:/login";
    }

    String empId = (String) session.getAttribute("PENDING_OTP_EMP_ID");
    boolean valid = hrmsService.verifyOtp(empId, otp);
    if (!valid) {
      logger.warn("[UCO SSO] Failed OTP verification attempt for employee {}", empId);
      return "redirect:/otp?error=otp";
    }

    HrmsUserProfile profile = hrmsService.fetchUserProfile(empId);
    if (profile == null) {
      return "redirect:/login?error=invalid_employee";
    }

    AppUser appUser = hrmsService.syncAppUser(profile);

    UserDetails userDetails = new User(
        appUser.username,
        "",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.role))
    );
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
    } else if ("invalid_employee".equals(reason)) {
      errorMessage = "Employee Authorization Failure: Your employee record could not be verified in the bank HRMS database. Please contact DIT SWDC Cell.";
    }
    model.addAttribute("errorMessage", errorMessage);
    return "error";
  }

  private void populateBankSessionAttributes(HttpSession session, HrmsUserProfile profile) {
    session.setAttribute("emp_id", profile.getEmpNo());
    session.setAttribute("sol_id", profile.getPostingSolId());
    session.setAttribute("UEMPNO", profile.getEmpNo());
    session.setAttribute("USOL", profile.getPostingSolId());
    session.setAttribute("USER_NAME", profile.getName());
    session.setAttribute("USER_ROLE", profile.getResolvedRole());
    session.setAttribute("USER_ZONE", profile.getResolvedZone());
    session.setAttribute("CADRE", profile.getCadre());
    session.setAttribute("SCALE", profile.getScaleCode());
    session.setAttribute("login_time", LocalDateTime.now());
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

