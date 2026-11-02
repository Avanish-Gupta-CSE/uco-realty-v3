package com.ucorealty.internal.controller;

import com.ucorealty.entity.AppUser;
import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.Lead;
import com.ucorealty.entity.LoginAudit;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.repository.UserRepository;
import com.ucorealty.internal.security.PasswordPolicy;
import com.ucorealty.internal.security.RoleCheck;
import com.ucorealty.service.AuditService;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminWebController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminWebController.class);

  private final ProjectService projectService;
  private final ProjectRepository projectRepo;
  private final ProjectImageRepository imageRepo;
  private final UserRepository userRepo;
  private final LeadRepository leadRepo;
  private final MasterDataService masterDataService;
  private final PasswordEncoder passwordEncoder;
  private final LeadExportController leadExportController;
  private final AuditService auditService;
  private final com.ucorealty.hrms.HrmsService hrmsService;

  public AdminWebController(ProjectService projectService, ProjectRepository projectRepo,
                            ProjectImageRepository imageRepo, UserRepository userRepo,
                            LeadRepository leadRepo, MasterDataService masterDataService,
                            PasswordEncoder passwordEncoder, LeadExportController leadExportController,
                            AuditService auditService) {
    this(projectService, projectRepo, imageRepo, userRepo, leadRepo, masterDataService,
         passwordEncoder, leadExportController, auditService, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public AdminWebController(ProjectService projectService, ProjectRepository projectRepo,
                            ProjectImageRepository imageRepo, UserRepository userRepo,
                            LeadRepository leadRepo, MasterDataService masterDataService,
                            PasswordEncoder passwordEncoder, LeadExportController leadExportController,
                            AuditService auditService,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) com.ucorealty.hrms.HrmsService hrmsService) {
    this.projectService = projectService;
    this.projectRepo = projectRepo;
    this.imageRepo = imageRepo;
    this.userRepo = userRepo;
    this.leadRepo = leadRepo;
    this.masterDataService = masterDataService;
    this.passwordEncoder = passwordEncoder;
    this.leadExportController = leadExportController;
    this.auditService = auditService;
    this.hrmsService = hrmsService;
  }

  // --- Project Directory & Workflow Queue ---

  @GetMapping({"", "/projects"})
  public String listProjects(@RequestParam(required = false) String status,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) String zone,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Authentication auth, Model model) {
    String username = auth.getName();
    AppUser user = userRepo.findByUsername(username).orElse(null);
    List<Project> list = new ArrayList<>(projectService.adminList(username, "jurisdiction"));

    // 1. Status Filter
    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
      String finalStatus = status.trim();
      list = list.stream()
          .filter(p -> p.status != null && p.status.equalsIgnoreCase(finalStatus))
          .collect(Collectors.toList());
    }

    // 2. Zone Filter (for National Staff roles)
    if (zone != null && !zone.trim().isEmpty() && !"ALL".equalsIgnoreCase(zone.trim())) {
      String finalZone = zone.trim();
      list = list.stream()
          .filter(p -> p.zoneName != null && p.zoneName.equalsIgnoreCase(finalZone))
          .collect(Collectors.toList());
    }

    // 3. Search Filter across ID, Name, Builder, RERA, City, State, Branch, Zone
    if (search != null && !search.trim().isEmpty()) {
      String q = search.trim().toLowerCase();
      list = list.stream()
          .filter(p -> (p.id != null && p.id.toLowerCase().contains(q))
              || (p.projectName != null && p.projectName.toLowerCase().contains(q))
              || (p.builderName != null && p.builderName.toLowerCase().contains(q))
              || (p.reraNo != null && p.reraNo.toLowerCase().contains(q))
              || (p.cityName != null && p.cityName.toLowerCase().contains(q))
              || (p.stateName != null && p.stateName.toLowerCase().contains(q))
              || (p.branchName != null && p.branchName.toLowerCase().contains(q))
              || (p.zoneName != null && p.zoneName.toLowerCase().contains(q)))
          .collect(Collectors.toList());
    }

    // 4. Sort by latest activity (updatedAt or createdAt descending)
    list.sort((a, b) -> {
      java.time.LocalDateTime ta = a.updatedAt != null ? a.updatedAt : a.createdAt;
      java.time.LocalDateTime tb = b.updatedAt != null ? b.updatedAt : b.createdAt;
      if (ta == null && tb == null) return 0;
      if (ta == null) return 1;
      if (tb == null) return -1;
      return tb.compareTo(ta);
    });

    // 5. Pagination
    int totalElements = list.size();
    if (size <= 0) size = 10;
    int totalPages = (totalElements == 0) ? 1 : (int) Math.ceil((double) totalElements / size);
    if (page < 0) page = 0;
    if (page >= totalPages && totalPages > 0) page = totalPages - 1;

    int fromIndex = Math.min(page * size, totalElements);
    int toIndex = Math.min(fromIndex + size, totalElements);
    List<Project> pageContent = list.subList(fromIndex, toIndex);

    Page<Project> projectPage = new PageImpl<>(pageContent, PageRequest.of(page, size), totalElements);

    model.addAttribute("projectPage", projectPage);
    model.addAttribute("projects", projectPage.getContent());
    model.addAttribute("currentStatus", (status != null && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : "");
    model.addAttribute("currentSearch", search != null ? search.trim() : "");
    model.addAttribute("currentZone", (zone != null && !"ALL".equalsIgnoreCase(zone.trim())) ? zone.trim() : "");
    model.addAttribute("zones", masterDataService.getZones());
    model.addAttribute("userRole", user != null ? user.role : "");
    model.addAttribute("userZone", user != null ? user.zone : "");
    return "admin-projects";
  }

  @GetMapping("/projects/new")
  public String newProjectForm(Authentication auth, Model model) {
    Project p = new Project();
    AppUser user = userRepo.findByUsername(auth.getName()).orElse(null);
    if (user != null && "ZO_MAKER".equals(user.role) && user.zone != null) {
      p.zoneName = user.zone.trim();
      p.branchName = user.branch;
      model.addAttribute("userZone", user.zone.trim());
    }

    model.addAttribute("project", p);
    model.addAttribute("zones", masterDataService.getZones());
    model.addAttribute("states", masterDataService.getStates());
    model.addAttribute("existingImages", Collections.emptyList());
    model.addAttribute("existingCount", 0);
    model.addAttribute("initialVisible", 4);
    return "project-form";
  }

  @GetMapping("/projects/{id}/edit")
  public String editProjectForm(@PathVariable String id, Authentication auth, Model model) {
    Project p = projectService.findById(id);
    AppUser user = userRepo.findByUsername(auth.getName()).orElse(null);
    if (user != null && "ZO_MAKER".equals(user.role) && user.zone != null) {
      model.addAttribute("userZone", user.zone.trim());
    }

    List<ProjectImage> existingImages = imageRepo.findByProjectId(id);
    int existingCount = (existingImages != null) ? existingImages.size() : 0;
    int initialVisible = Math.max(existingCount, 4);

    model.addAttribute("project", p);
    model.addAttribute("zones", masterDataService.getZones());
    model.addAttribute("states", masterDataService.getStates());
    model.addAttribute("existingImages", existingImages != null ? existingImages : Collections.emptyList());
    model.addAttribute("existingCount", existingCount);
    model.addAttribute("initialVisible", initialVisible);
    return "project-form";
  }

  @PostMapping("/projects/save")
  public String saveProject(@ModelAttribute Project formProject,
                            @RequestParam(defaultValue = "draft") String action,
                            @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                            @RequestParam(value = "photo1", required = false) MultipartFile photo1,
                            @RequestParam(value = "photo2", required = false) MultipartFile photo2,
                            @RequestParam(value = "photo3", required = false) MultipartFile photo3,
                            @RequestParam(value = "photo4", required = false) MultipartFile photo4,
                            @RequestParam(value = "photo5", required = false) MultipartFile photo5,
                            @RequestParam(value = "photo6", required = false) MultipartFile photo6,
                            @RequestParam(value = "photo7", required = false) MultipartFile photo7,
                            @RequestParam(value = "photo8", required = false) MultipartFile photo8,
                            @RequestParam(value = "photo9", required = false) MultipartFile photo9,
                            @RequestParam(value = "photo10", required = false) MultipartFile photo10,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirectAttrs) {
    Project saved = null;
    try {
      String username = auth.getName();

      if (formProject.id != null && !formProject.id.trim().isEmpty()) {
        saved = projectService.updateDraft(formProject.id.trim(), formProject, username);
      } else {
        saved = projectService.saveDraft(formProject, username);
      }
      formProject.id = saved.id;

      // Collect photos from individual slots (photo1..photo10) or multi-input photos
      List<MultipartFile> incomingFiles = new ArrayList<>();
      if (photos != null) {
        for (MultipartFile f : photos) {
          if (f != null && !f.isEmpty()) incomingFiles.add(f);
        }
      }
      for (MultipartFile f : Arrays.asList(photo1, photo2, photo3, photo4, photo5, photo6, photo7, photo8, photo9, photo10)) {
        if (f != null && !f.isEmpty() && !incomingFiles.contains(f)) {
          incomingFiles.add(f);
        }
      }

      // Handle photos upload (limit to 10 photos max per project, up to 2MB each)
      if (!incomingFiles.isEmpty()) {
        int existingCount = imageRepo.findByProjectId(saved.id).size();
        for (MultipartFile file : incomingFiles) {
          if (existingCount >= 10) {
            break; // Limit to maximum 10 photos total per project
          }
          if (file.getSize() > 2L * 1024 * 1024) continue; // skip over 2MB
          String ext = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
              ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')).toLowerCase() : ".jpg";
          if (!Arrays.asList(".jpg", ".jpeg", ".png", ".webp").contains(ext)) continue;

          byte[] bytes = file.getBytes();
          String mime = FileController.sniffMime(bytes);
          if (mime == null || !FileController.extensionMatches(ext, mime)) continue;

          ProjectImage img = new ProjectImage();
          img.projectId = saved.id;
          img.fileName = FileController.cleanName(file.getOriginalFilename());
          img.mimeType = mime;
          img.fileSize = file.getSize();
          img.fileData = bytes;
          img.uploadedBy = username;
          img.isVerified = 1;
          img.filePath = "/files/pending/" + img.fileName;
          img = imageRepo.save(img);
          img.filePath = "/files/" + img.id;
          imageRepo.save(img);
          existingCount++;
        }
        saved.totalImgCount = imageRepo.findByProjectId(saved.id).size();
        projectRepo.save(saved);
      }

      // Handle immediate submission if requested
      if ("submit".equalsIgnoreCase(action)) {
        projectService.submit(saved.id, username);
        redirectAttrs.addFlashAttribute("message", "Project " + saved.projectName + " saved and submitted for Checker approval.");
      } else {
        redirectAttrs.addFlashAttribute("message", "Draft saved for " + saved.projectName + ".");
      }

      return "redirect:/admin/projects";
    } catch (Exception e) {
      log.error("Failed to save project: {}", e.getMessage(), e);
      String msg = e.getMessage();
      if (e instanceof org.springframework.web.server.ResponseStatusException) {
        org.springframework.web.server.ResponseStatusException rse = (org.springframework.web.server.ResponseStatusException) e;
        if (rse.getReason() != null) {
          msg = rse.getReason();
        }
      } else {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
          root = root.getCause();
        }
        if (root.getMessage() != null && !root.getMessage().trim().isEmpty()) {
          msg = root.getMessage();
        }
      }
      model.addAttribute("error", msg);
      model.addAttribute("project", formProject);
      model.addAttribute("zones", masterDataService.getZones());
      model.addAttribute("states", masterDataService.getStates());

      AppUser user = userRepo.findByUsername(auth.getName()).orElse(null);
      if (user != null && "ZO_MAKER".equals(user.role) && user.zone != null) {
        model.addAttribute("userZone", user.zone.trim());
      }

      String checkId = formProject.id != null ? formProject.id.trim() : (saved != null ? saved.id : null);
      List<ProjectImage> existingImages = (checkId != null && !checkId.isEmpty())
          ? imageRepo.findByProjectId(checkId) : Collections.emptyList();
      int existingCount = existingImages != null ? existingImages.size() : 0;
      model.addAttribute("existingImages", existingImages != null ? existingImages : Collections.emptyList());
      model.addAttribute("existingCount", existingCount);
      model.addAttribute("initialVisible", Math.max(existingCount, 4));

      return "project-form";
    }
  }

  @PostMapping("/projects/{id}/submit")
  public String submitProject(@PathVariable String id, Authentication auth, RedirectAttributes redirectAttrs) {
    try {
      projectService.submit(id, auth.getName());
      redirectAttrs.addFlashAttribute("message", "Project submitted for approval.");
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Submission failed: " + e.getMessage());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("/projects/{projectId}/images/{imageId}/delete")
  public String deleteProjectImage(@PathVariable String projectId,
                                   @PathVariable Long imageId,
                                   Authentication auth,
                                   RedirectAttributes redirectAttrs) {
    try {
      String username = auth.getName();
      projectService.requireEditableProject(projectId, username);

      ProjectImage img = imageRepo.findById(imageId)
          .filter(i -> projectId.equals(i.projectId))
          .orElse(null);
      if (img != null) {
        imageRepo.delete(img);
        Project p = projectService.findById(projectId);
        p.totalImgCount = imageRepo.findByProjectId(projectId).size();
        projectRepo.save(p);
        redirectAttrs.addFlashAttribute("message", "Photo deleted successfully.");
      }
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Could not delete photo: " + e.getMessage());
    }
    return "redirect:/admin/projects/" + projectId + "/edit";
  }

  @PostMapping("/projects/{id}/decide")
  public String decideProject(@PathVariable String id,
                              @RequestParam String decision,
                              @RequestParam(required = false) String comment,
                              Authentication auth,
                              RedirectAttributes redirectAttrs) {
    try {
      projectService.decide(id, decision, auth.getName(), comment);
      redirectAttrs.addFlashAttribute("message", "Decision (" + decision + ") recorded successfully.");
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Decision failed: " + e.getMessage());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping({"/projects/{id}/reduce-units", "/projects/{id}/reduce-inventory"})
  public String reduceInventory(@PathVariable String id,
                                @RequestParam int units,
                                @RequestParam String reason,
                                Authentication auth,
                                RedirectAttributes redirectAttrs) {
    try {
      projectService.reduceInventory(id, units, reason, auth.getName());
      auditService.logActivity(auth.getName(), "REDUCE_UNITS", "PROJECT", id,
          "Reduced available units by " + units + ". Reason: " + reason.trim());
      redirectAttrs.addFlashAttribute("message", "Available inventory reduced by " + units + " units.");
    } catch (Exception e) {
      String errMsg = (e instanceof org.springframework.web.server.ResponseStatusException &&
          ((org.springframework.web.server.ResponseStatusException) e).getReason() != null)
          ? ((org.springframework.web.server.ResponseStatusException) e).getReason()
          : e.getMessage();
      redirectAttrs.addFlashAttribute("error", "Inventory reduction failed: " + errMsg);
    }
    return "redirect:/admin/projects";
  }

  // --- Customer Leads Management ---

  @GetMapping("/leads")
  public String listLeads(@RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String search,
                          Model model) {
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

  // --- Password Self Change ---

  @GetMapping("/change-password")
  public String changePasswordPage() {
    return "change-password";
  }

  @PostMapping("/change-password")
  public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               Authentication auth,
                               RedirectAttributes redirectAttrs) {
    try {
      AppUser u = userRepo.findByUsername(auth.getName()).orElseThrow(() -> new IllegalStateException("User not found"));
      if (!passwordEncoder.matches(currentPassword, u.passwordHash)) {
        redirectAttrs.addFlashAttribute("error", "Current password is incorrect.");
        return "redirect:/admin/change-password";
      }
      if (currentPassword.equals(newPassword)) {
        redirectAttrs.addFlashAttribute("error", "New password must be different from current password.");
        return "redirect:/admin/change-password";
      }
      PasswordPolicy.check(newPassword, u.username);
      u.passwordHash = passwordEncoder.encode(newPassword);
      userRepo.save(u);

      auditService.logActivity(auth.getName(), "CHANGE_PASSWORD", "USER", auth.getName(), "User changed account password");

      redirectAttrs.addFlashAttribute("message", "Password changed successfully.");
      return "redirect:/admin/projects";
    } catch (Exception e) {
      redirectAttrs.addFlashAttribute("error", "Password change failed: " + e.getMessage());
      return "redirect:/admin/change-password";
    }
  }

  // --- System Audit Trails & Activity Logs (Strictly Superadmin for DIT only) ---

  @GetMapping("/audit-logs")
  public String viewAuditLogs(@RequestParam(value = "pageActivity", required = false, defaultValue = "0") int pageActivity,
                              @RequestParam(value = "pageLogin", required = false, defaultValue = "0") int pageLogin,
                              @RequestParam(value = "tab", required = false, defaultValue = "activity") String activeSubTab,
                              Model model,
                              Authentication auth) {
    RoleCheck.require(auth, "SUPERADMIN_DIT");

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
