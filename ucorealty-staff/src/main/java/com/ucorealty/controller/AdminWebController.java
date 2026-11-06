package com.ucorealty.controller;

import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.Lead;
import com.ucorealty.entity.LoginAudit;
import com.ucorealty.entity.Project;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.security.RoleCheck;
import com.ucorealty.security.Roles;
import com.ucorealty.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Customer leads page + Excel export, and the DIT-only Logs page. (Project screens: ProjectWorkflowController.) */
@Controller
@RequestMapping("/admin")
public class AdminWebController {
  private static final int LEADS_PER_PAGE = 20;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminWebController.class);
  private final ProjectRepository projectRepo;
  private final LeadRepository leadRepo;
  private final LeadExportController leadExportController;
  private final AuditService auditService;

  public AdminWebController(ProjectRepository projectRepo, LeadRepository leadRepo,
                            LeadExportController leadExportController, AuditService auditService) {
    this.projectRepo = projectRepo;
    this.leadRepo = leadRepo;
    this.leadExportController = leadExportController;
    this.auditService = auditService;
  }

  // --- Customer Leads Management ---

  @GetMapping("/leads")
  public String listLeads(@RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "1") int pageNo,
                          Authentication auth,
                          Model model) {
    // The nav link is hidden for other roles, but hiding is not protection: customer phone numbers need a real check
    RoleCheck.require(auth, Roles.LEAD_ROLES);
    List<Lead> leads = leadExportController.filterLeads(from, to, status, search);

    int total = leads.size();
    int pages = Math.max(1, (total + LEADS_PER_PAGE - 1) / LEADS_PER_PAGE);
    int current = Math.min(Math.max(1, pageNo), pages);
    leads = leads.subList((current - 1) * LEADS_PER_PAGE, Math.min(current * LEADS_PER_PAGE, total));
    model.addAttribute("leadCount", total);
    model.addAttribute("pageCount", pages);
    model.addAttribute("pageNo", current);

    // Map project IDs to project names for friendly display
    Map<String, String> projectNames = new HashMap<>();
    for (Project p : projectRepo.findAll()) {
      projectNames.put(p.id, p.projectName);
    }

    model.addAttribute("leads", leads);
    model.addAttribute("projectNames", projectNames);
    return "leads";
  }

  @GetMapping("/leads/export.xlsx")
  public void exportLeads(@RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String search,
                          jakarta.servlet.http.HttpServletResponse res,
                          Authentication auth) throws IOException {
    RoleCheck.require(auth, Roles.LEAD_ROLES);   // before the audit row: a refused export must not be recorded as an export
    auditService.logActivity(auth.getName(), "EXPORT_LEADS", "LEAD", "EXCEL", "Exported leads to Excel");
    leadExportController.export(from, to, status, search, res, auth);
  }

  // --- System Audit Trails & Activity Logs (Strictly Superadmin for DIT only) ---

  @GetMapping("/audit-logs")
  public String viewAuditLogs(@RequestParam(value = "pageActivity", required = false, defaultValue = "0") int pageActivity,
                              @RequestParam(value = "pageLogin", required = false, defaultValue = "0") int pageLogin,
                              @RequestParam(value = "tab", required = false, defaultValue = "activity") String activeSubTab,
                              Model model,
                              Authentication auth) {
    RoleCheck.require(auth, "SUPERADMIN_DIT");
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
