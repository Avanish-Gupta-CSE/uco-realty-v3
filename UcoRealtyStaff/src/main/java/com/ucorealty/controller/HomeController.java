package com.ucorealty.controller;

import com.ucorealty.model.HrmsEmployee;
import com.ucorealty.model.StaffUser;
import com.ucorealty.service.AuditService;
import com.ucorealty.service.HrmsService;
import com.ucorealty.util.AttemptLimiter;
import com.ucorealty.util.SsoToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.GeneralSecurityException;
import java.time.Duration;

/**
 * Staff login. The bank SSO portal sends the employee to /entry?Value=<encrypted>; this class decrypts it, checks the employee
 * in HRMS, stores emp_id and sol_id in the session and goes to /home. Without those attributes /home goes to the login page.
 * Every other staff page is guarded by SessionInterceptor. In the h2 and uat profiles the login page also offers the test login.
 * Change this file when: the sign-in steps change (bank SSO entry, test login, logout, error page).
 */
@Controller
public class HomeController {

  private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

  // The bank's SSO key, as plain text. No default on purpose: the application refuses to start without a valid one (init below).
  @Value("${sso.encryption.key}")
  private String encryptionKey;

  // How the bank portal encrypts the token: AES/GCM/NoPadding (default) or AES/CBC/PKCS5Padding. Anything else stops the start.
  @Value("${sso.cipher:AES/GCM/NoPadding}")
  private String ssoCipher;

  // Test login (employee number + test OTP) on the login page: only the h2 and uat profiles turn this on.
  @Value("${uco.sso.dev-login-enabled:false}")
  private boolean devLoginEnabled;

  // Test login only (it exists on UAT and laptops, never in production): guessing or requesting the OTP again and again is limited.
  // Wrong OTPs: 5 per employee in 15 minutes, then that employee id is locked until the oldest wrong try is 15 minutes old.
  // OTP requests ("resend"): 20 per employee and 100 per network address in 15 minutes.
  private static final Duration WINDOW = Duration.ofMinutes(15);
  private final AttemptLimiter wrongOtpByEmployee = new AttemptLimiter(5, WINDOW);
  private final AttemptLimiter otpRequestsByEmployee = new AttemptLimiter(20, WINDOW);
  private final AttemptLimiter otpRequestsByAddress = new AttemptLimiter(100, WINDOW);

  private final HrmsService hrmsService;
  private final AuditService auditService;

  public HomeController(HrmsService hrmsService, AuditService auditService) {
    this.hrmsService = hrmsService;
    this.auditService = auditService;
  }

  @PostConstruct
  void init() {
    if (!SsoToken.isValidCipher(ssoCipher)) {
      throw new IllegalStateException("sso.cipher must be exactly AES/GCM/NoPadding or AES/CBC/PKCS5Padding");
    }
    if (!SsoToken.isValidKey(encryptionKey)) {
      throw new IllegalStateException("sso.encryption.key (SSO_ENCRYPTION_KEY) must be 16, 24 or 32 characters long");
    }
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/home";
  }

  /** Where the bank SSO portal sends the employee: /entry?Value=<encrypted emp_id and sol_id>. */
  @GetMapping("/entry")
  public String entry(@RequestParam(value = "Value", required = false) String valueCapital,
                      @RequestParam(value = "value", required = false) String valueSmall, HttpServletRequest request) {
    // the portal may send the parameter as Value or as value
    String value = valueCapital != null ? valueCapital : valueSmall;
    if (value == null || value.trim().isEmpty()) {
      return "redirect:/home";
    }
    String empNo;
    String sol;
    try {
      String text = SsoToken.decrypt(value, encryptionKey, ssoCipher);
      empNo = SsoToken.employeeNumber(text);
      sol = SsoToken.solId(text);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      logger.warn("[UCO SSO /entry] Token could not be decrypted: {}", e.getMessage());
      auditService.logLoginAttempt("UNKNOWN", extractClientIp(request), false, "SSO token could not be decrypted");
      return "redirect:/error-page?reason=unauthorized";
    }
    if (empNo == null) {
      logger.warn("[UCO SSO /entry] Decrypted token has no employee number");
      auditService.logLoginAttempt("UNKNOWN", extractClientIp(request), false, "SSO token decrypted but has no employee number");
      return "redirect:/error-page?reason=unauthorized";
    }
    logger.info("[UCO SSO /entry] Token accepted for employee {} (token SOL {})", empNo, sol);
    return authenticateAndEstablishSession(empNo, request, "Bank SSO");
  }

  /** The landing page after login: the project dashboard if the session is set, otherwise the login page. */
  @GetMapping("/home")
  public String home(HttpSession session) {
    boolean signedIn = session.getAttribute("emp_id") != null && session.getAttribute("sol_id") != null;
    return signedIn ? "redirect:/admin/projects" : "redirect:/login";
  }

  /**
   * Core helper to authenticate employee against HRMS, build the signed-in user, and set the session attributes.
   */
  private String authenticateAndEstablishSession(String empId, HttpServletRequest request, String loginMechanism) {
    HrmsEmployee profile = hrmsService.fetchUserProfile(empId);
    if (profile == null || !profile.isActive()) {
      logger.warn("[UCO SSO] Employee record {} could not be verified in HRMS.", empId);
      auditService.logLoginAttempt(empId, extractClientIp(request), false, loginMechanism + " - employee not found/inactive in HRMS");
      return "redirect:/error-page?reason=invalid_employee";
    }

    // Role and zone come from the HRMS posting, never from the URL.
    if (hrmsService.resolveRole(profile) == null) {
      logger.warn("[UCO SSO] Employee {} (SOL {}) has no role in this portal.", profile.getEmpNo(), profile.getPostingSolId());
      auditService.logLoginAttempt(empId, extractClientIp(request), false,
          loginMechanism + " - not authorised for this portal (SOL " + profile.getPostingSolId() + ")");
      return "redirect:/error-page?reason=not_authorized";
    }

    StaffUser appUser = hrmsService.toAppUser(profile);
    HttpSession session = freshSession(request);

    Authentication auth = new UsernamePasswordAuthenticationToken(appUser, null, appUser.getAuthorities());
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

    return "redirect:/home";
  }

  /**
   * SSO Login Landing Page for direct login and OTP entry.
   */
  /** Someone typed /logout in the address bar. Logging out needs the Logout button (a POST with a security token), so just go home. */
  @GetMapping("/logout")
  public String logoutByAddress() {
    return "redirect:/home";
  }

  @GetMapping("/login")
  public String loginPage(Authentication auth, Model model) {
    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
      return "redirect:/home";
    }
    model.addAttribute("devLogin", devLoginEnabled);
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
    String address = request.getRemoteAddr();   // the real connection, not a header the caller can write
    if (otpRequestsByEmployee.isBlocked(normalized) || otpRequestsByAddress.isBlocked(address)) {
      logger.warn("[UCO SSO] Too many OTP requests for employee {} from {}: refused", normalized, address);
      auditService.logLoginAttempt(normalized, extractClientIp(request), false, "Test login - too many OTP requests");
      return "redirect:/login?error=locked";
    }
    otpRequestsByEmployee.record(normalized);
    otpRequestsByAddress.record(address);
    HrmsEmployee profile = hrmsService.fetchUserProfile(normalized);
    if (profile == null || !profile.isActive()) {
      logger.warn("[UCO SSO] OTP requested for non-existent or inactive employee: {}", normalized);
      return "redirect:/login?error=invalid_employee";
    }

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
    if (wrongOtpByEmployee.isBlocked(empId)) {
      forgetPending(session);
      logger.warn("[UCO SSO] Employee {} is locked after too many wrong OTPs", empId);
      return "redirect:/login?error=locked";
    }
    boolean valid = hrmsService.verifyOtp(empId, otp);
    if (!valid) {
      wrongOtpByEmployee.record(empId);
      logger.warn("[UCO SSO] Failed OTP verification attempt for employee {}", empId);
      auditService.logLoginAttempt(empId, extractClientIp(request), false, "Test login - wrong OTP");
      if (wrongOtpByEmployee.isBlocked(empId)) {
        forgetPending(session);
        return "redirect:/login?error=locked";
      }
      return "redirect:/otp?error=otp";
    }
    wrongOtpByEmployee.reset(empId);

    session.removeAttribute("PENDING_OTP_EMP_ID");
    session.removeAttribute("PENDING_OTP_NAME");
    session.removeAttribute("PENDING_OTP_SOL");
    session.removeAttribute("PENDING_OTP_MOBILE");
    session.removeAttribute("PENDING_OTP_ROLE");
    return authenticateAndEstablishSession(empId, request, "Test login");
  }

  /**
   * Custom Access Denied / Security Authentication Error Page.
   */
  @GetMapping("/error-page")
  public String errorPage(@RequestParam(value = "reason", required = false) String reason, Model model) {
    logger.warn("[UCO Portal] Rendering security error page with reason code: {}", reason);
    String errorMessage = "Please open the Realty portal from the UCO Bank intranet portal.";
    if ("unauthorized".equals(reason)) {
      errorMessage = "We could not verify your sign-in link. Please open the Realty portal again from the UCO Bank intranet portal.";
    } else if ("not_authorized".equals(reason)) {
      errorMessage = "Your posting (SOL) does not have access to the Realty portal. Please contact the DIT SWDC Cell if you need access.";
    } else if ("invalid_employee".equals(reason)) {
      errorMessage = "Your employee record could not be verified in HRMS. Please contact the DIT SWDC Cell.";
    }
    model.addAttribute("errorMessage", errorMessage);
    return "error";
  }

  /** The SSO gives us only the employee number and SOL; role and zone come from HRMS and travel in the principal. */
  private void populateBankSessionAttributes(HttpSession session, HrmsEmployee profile) {
    session.setAttribute("emp_id", profile.getEmpNo());
    session.setAttribute("emp_name", profile.getName());   // shown in the header
    session.setAttribute("sol_id", profile.getPostingSolId());
  }

  private static void forgetPending(HttpSession session) {
    for (String key : new String[] {"PENDING_OTP_EMP_ID", "PENDING_OTP_NAME", "PENDING_OTP_SOL", "PENDING_OTP_MOBILE", "PENDING_OTP_ROLE"}) {
      session.removeAttribute(key);
    }
  }

  /** New session id for every login (prevents session fixation). */
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

