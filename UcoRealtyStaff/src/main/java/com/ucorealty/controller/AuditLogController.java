package com.ucorealty.controller;

import com.ucorealty.model.AuditLog;
import com.ucorealty.model.LoginAudit;
import com.ucorealty.service.AuditService;
import com.ucorealty.util.StaffRoles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * The Logs page (what staff did, and who signed in), for DIT only. Change this file when: the Logs page shows other data.
 * The two lists are read by AuditService; the page itself is audit-logs.jsp.
 */
@Controller
@RequestMapping("/admin")
public class AuditLogController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditLogController.class);
  private final AuditService auditService;

  public AuditLogController(AuditService auditService) {
    this.auditService = auditService;
  }

  @GetMapping("/audit-logs")
  public String viewAuditLogs(@RequestParam(value = "pageActivity", required = false, defaultValue = "0") int pageActivity,
                              @RequestParam(value = "pageLogin", required = false, defaultValue = "0") int pageLogin,
                              @RequestParam(value = "tab", required = false, defaultValue = "activity") String activeSubTab,
                              Model model,
                              Authentication auth) {
    StaffRoles.require(auth, "SUPERADMIN_DIT");
    log.info("[UCO AUDIT] {} opened the audit logs (tab {})", auth.getName(), activeSubTab);

    int pageSize = 15;
    Page<AuditLog> activityLogs = auditService.getActivityLogs(PageRequest.of(Math.max(0, pageActivity), pageSize));
    Page<LoginAudit> loginAudits = auditService.getLoginAudits(PageRequest.of(Math.max(0, pageLogin), pageSize));

    model.addAttribute("pageTitle", "System Audit Logs - UCO Bank Realty Portal");
    model.addAttribute("activeTab", "audit");
    model.addAttribute("activeSubTab", activeSubTab);
    model.addAttribute("activityLogs", activityLogs);
    model.addAttribute("loginAudits", loginAudits);
    model.addAttribute("pageActivity", pageActivity);
    model.addAttribute("pageLogin", pageLogin);

    return "audit-logs";
  }
}
