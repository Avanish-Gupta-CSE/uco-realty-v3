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
                          Authentication auth,
                          Model model) {
    // The nav link is hidden for other roles, but hiding is not protection: customer phone numbers need a real check
    RoleCheck.require(auth, Roles.LEAD_ROLES);
    List<Lead> leads = leadRepo.findAll();

    // In-memory date & status filter
    if (from != null && !from.trim().isEmpty()) {
      LocalDate f = LocalDate.parse(from.trim());
      leads = leads.stream().filter(l -> l.createdAt != null && !l.createdAt.toLocalDate().isBefore(f)).collect(Collectors.toList());
    }
    if (to != null && !to.trim().isEmpty()) {
      LocalDate t = LocalDate.parse(to.trim());
      leads = leads.stream().filter(l -> l.createdAt != null && !l.createdAt.toLocalDate().isAfter(t)).collect(Collectors.toList());
    }
    if (status != null && !status.trim().isEmpty()) {
      String st = status.trim();
      leads = leads.stream().filter(l -> {
        if (l.status == null) return false;
        if (st.equalsIgnoreCase(l.status)) return true;
        if ("Visitor".equalsIgnoreCase(st) && "VISITOR".equalsIgnoreCase(l.status)) return true;
        if ("Showed interest in Home Loan".equalsIgnoreCase(st) && "SUCCESSFUL_APPLIED".equalsIgnoreCase(l.status)) return true;
        return false;
      }).collect(Collectors.toList());
    }
    if (search != null && !search.trim().isEmpty()) {
      String q = search.trim().toLowerCase();
      leads = leads.stream().filter(l ->
          (l.customerName != null && l.customerName.toLowerCase().contains(q)) ||
          (l.phone != null && l.phone.contains(q))
      ).collect(Collectors.toList());
    }

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
