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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

/**
 * Staff login. The bank SSO portal sends the employee to /entry?Value=<encrypted>; this class decrypts it, checks the employee
 * in HRMS, stores emp_id and sol_id in the session and goes to /home. Without those attributes /home goes to the login page.
 * Every other staff page is guarded by SessionInterceptor. In the h2 and uat profiles the login page also offers the test login.
 */
@Controller
public class HomeController {

  private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

  // The bank's SSO key, as plain text. No default on purpose: the application refuses to start without a valid one (init below).
  @Value("${sso.encryption.key}")
  private String encryptionKey;

  // Test login (employee number + test OTP) on the login page: only the h2 and uat profiles turn this on.
  @Value("${uco.sso.dev-login-enabled:false}")
  private boolean devLoginEnabled;

  private final HrmsService hrmsService;
  private final AuditService auditService;

  public HomeController(HrmsService hrmsService, AuditService auditService) {
    this.hrmsService = hrmsService;
    this.auditService = auditService;
  }

  @PostConstruct
  void init() {
    if (!SsoCryptoUtil.isValidKey(encryptionKey)) {
      throw new IllegalStateException("sso.encryption.key (SSO_ENCRYPTION_KEY) must be 16, 24 or 32 characters long");
    }
  }

  @GetMapping("/")
  public String root() {
    return "redirect:/home";
  }

  /** Where the bank SSO portal sends the employee: /entry?Value=<encrypted emp_id and sol_id>. */
  @GetMapping("/entry")
  public String entry(@RequestParam(value = "Value", required = false) String value, HttpServletRequest request) {
    if (value == null || value.trim().isEmpty()) {
      return "redirect:/home";
    }
    String empNo;
    String sol;
    try {
      String text = SsoCryptoUtil.decrypt(value, encryptionKey);
      empNo = SsoCryptoUtil.employeeNumber(text);
      sol = SsoCryptoUtil.solId(text);
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
    HrmsUserProfile profile = hrmsService.fetchUserProfile(empId);
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

    return "redirect:/home";
  }

  /**
   * SSO Login Landing Page for direct login and OTP entry.
   */
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
    HrmsUserProfile profile = hrmsService.fetchUserProfile(normalized);
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
    boolean valid = hrmsService.verifyOtp(empId, otp);
    if (!valid) {
      logger.warn("[UCO SSO] Failed OTP verification attempt for employee {}", empId);
      return "redirect:/otp?error=otp";
    }

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
  private void populateBankSessionAttributes(HttpSession session, HrmsUserProfile profile) {
    session.setAttribute("emp_id", profile.getEmpNo());
    session.setAttribute("sol_id", profile.getPostingSolId());
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

