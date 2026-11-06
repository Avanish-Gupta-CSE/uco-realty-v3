package com.ucorealty.controller;

import com.ucorealty.entity.AppUser;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectData;
import com.ucorealty.entity.ProjectForm;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.entity.ProjectStaging;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.security.CurrentUser;
import com.ucorealty.security.RoleCheck;
import com.ucorealty.security.Roles;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.ProjectRow;
import com.ucorealty.service.ProjectService;
import com.ucorealty.service.ProjectWorkflowService;
import com.ucorealty.util.PhotoFiles;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Every project screen. The maker works on his own copy (staging row); the checker reviews it; only an approval
 * touches the master table (see ProjectWorkflowService).
 */
@Controller
@RequestMapping("/admin")
public class ProjectWorkflowController {
  private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowController.class);
  private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

  private final ProjectWorkflowService workflow;
  private final ProjectService projectService;
  private final ProjectImageRepository imageRepo;
  private final MasterDataService masterData;

  public ProjectWorkflowController(ProjectWorkflowService workflow, ProjectService projectService,
                                   ProjectImageRepository imageRepo, MasterDataService masterData) {
    this.workflow = workflow;
    this.projectService = projectService;
    this.imageRepo = imageRepo;
    this.masterData = masterData;
  }

  // ================================================================================ dashboard

  @GetMapping({"", "/projects"})
  public String dashboard(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String zone,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          Authentication auth, Model model) {
    String username = auth.getName();
    AppUser user = CurrentUser.find(username);
    List<ProjectRow> rows = new ArrayList<>(workflow.dashboard(username));

    if (zone != null && !zone.trim().isEmpty() && !"ALL".equalsIgnoreCase(zone.trim())) {
      String z = zone.trim();
      rows = rows.stream().filter(r -> r.zoneName != null && r.zoneName.equalsIgnoreCase(z)).collect(Collectors.toList());
    }
    if (search != null && !search.trim().isEmpty()) {
      String q = search.trim().toLowerCase();
      rows = rows.stream().filter(r -> has(r.id, q) || has(r.projectName, q) || has(r.builderName, q) || has(r.reraNo, q)
          || has(r.cityName, q) || has(r.stateName, q) || has(r.branchName, q) || has(r.zoneName, q)).collect(Collectors.toList());
    }

    // The number on each status tab (within the current search and zone), and what waits for this user's decision
    Map<String, Long> counts = new HashMap<>();
    counts.put("ALL", (long) rows.size());
    for (String st : List.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "NEEDS_CORRECTION", "REJECTED", "CHANGE_PENDING")) {
      counts.put(st, rows.stream().filter(r -> hasStatus(r, st)).count());
    }
    model.addAttribute("statusCounts", counts);
    boolean checker = user != null && !"ZO_MAKER".equals(user.role) && !"HO_RETAIL".equals(user.role);
    model.addAttribute("waitingForMe", checker
        ? rows.stream().filter(r -> r.isWaitingForChecker() && !username.equals(r.getStagingOwner())).count() : 0L);

    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
      String st = status.trim();
      rows = rows.stream().filter(r -> hasStatus(r, st)).collect(Collectors.toList());
    }
    rows.sort((a, b) -> {
      if (a.updatedAt == null && b.updatedAt == null) return 0;
      if (a.updatedAt == null) return 1;
      if (b.updatedAt == null) return -1;
      return b.updatedAt.compareTo(a.updatedAt);
    });

    int total = rows.size();
    if (size <= 0) size = 10;
    int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / size);
    if (page < 0) page = 0;
    if (page >= totalPages) page = totalPages - 1;
    int from = Math.min(page * size, total);
    Page<ProjectRow> projectPage = new PageImpl<>(rows.subList(from, Math.min(from + size, total)), PageRequest.of(page, size), total);

    model.addAttribute("projectPage", projectPage);
    model.addAttribute("projects", projectPage.getContent());
    model.addAttribute("currentStatus", (status != null && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : "");
    model.addAttribute("currentSearch", search != null ? search.trim() : "");
    model.addAttribute("currentZone", (zone != null && !"ALL".equalsIgnoreCase(zone.trim())) ? zone.trim() : "");
    model.addAttribute("zones", masterData.getZones());
    model.addAttribute("userRole", user != null ? user.role : "");
    model.addAttribute("userZone", user != null ? user.zone : "");
    model.addAttribute("username", username);
    return "admin-projects";
  }

  private static boolean has(String v, String q) { return v != null && v.toLowerCase().contains(q); }

  private static boolean hasStatus(ProjectRow r, String st) {
    if ("CHANGE_PENDING".equalsIgnoreCase(st)) return r.getPendingKind() != null;      // live project with an edit / deletion waiting
    if ("PENDING_APPROVAL".equalsIgnoreCase(st)) return r.isWaitingForChecker();       // anything waiting for a checker
    return r.status != null && r.status.equalsIgnoreCase(st);
  }

  // ================================================================================ maker: forms

  /** Blank form for a NEW project. Nothing is stored until the maker presses Save Draft. */
  @GetMapping("/projects/new")
  public String newProject(Authentication auth, Model model) {
    RoleCheck.require(auth, Roles.MAKERS);
    ProjectForm blank = new ProjectForm();
    AppUser user = CurrentUser.of(auth);
    if (user != null && "ZO_MAKER".equals(user.role) && user.zone != null) {
      blank.zoneName = user.zone.trim();
      blank.branchName = user.branch;
    }
    return form(model, auth, blank, null, null);
  }

  /** The maker's own working copy (new project, or a change of a live project) opens again. */
  @GetMapping("/projects/staging/{stagingId}/edit")
  public String editStaging(@PathVariable Long stagingId, Authentication auth, Model model, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    ProjectStaging s = workflow.get(stagingId);
    if (!user.equals(s.createdBy)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the maker who raised this request can edit it");
    if (!ProjectStaging.DRAFT.equals(s.status) && !ProjectStaging.NEEDS_CORRECTION.equals(s.status)) {
      ra.addFlashAttribute("error", "It can be edited only while it is a draft or returned for correction (current: " + s.status + ")");
      return "redirect:/admin/projects";
    }
    return form(model, auth, s, s, null);
  }

  /** Opens (or creates) the maker's working copy of a LIVE project. The live project stays exactly as approved. */
  @GetMapping("/projects/{projectId}/change")
  public String changeLive(@PathVariable String projectId, Authentication auth, Model model, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      ProjectStaging s = workflow.openUpdateDraft(projectId, user);
      return form(model, auth, s, s, null);
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", e.getReason());
      return "redirect:/admin/projects";
    }
  }

  @PostMapping("/projects/staging/save")
  public String save(@ModelAttribute ProjectForm form,
                     @RequestParam(required = false) Long stagingId,
                     @RequestParam(defaultValue = "draft") String action,
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
                     @RequestParam(value = "customCity", required = false) String customCity,
                     HttpServletRequest request, Authentication auth, Model model, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    Set<String> sent = new HashSet<>(request.getParameterMap().keySet());
    ProjectStaging s = null;
    try {
      if (customCity != null && !customCity.trim().isEmpty()) {
        form.cityName = customCity.trim();
      } else if ("__OTHER__".equalsIgnoreCase(form.cityName)) {
        form.cityName = "";
      }
      s = stagingId == null ? workflow.createDraft(form, sent, user) : workflow.saveDraft(stagingId, form, sent, user);
      addPhotos(s, user, photo1, photo2, photo3, photo4, photo5, photo6, photo7, photo8, photo9, photo10);

      boolean update = ProjectStaging.UPDATE.equals(s.requestType);
      if ("submit".equalsIgnoreCase(action)) {
        workflow.submit(s.id, user);
        ra.addFlashAttribute("message", update
            ? "Change sent to the checker. The approved project stays live until it is approved."
            : "Project " + s.projectName + " sent to the checker for approval.");
      } else {
        ra.addFlashAttribute("message", update
            ? "Change saved as a draft. The approved project is unchanged and the checker does not see this draft yet."
            : "Draft saved for " + s.projectName + ". Nothing is live until a checker approves it.");
      }
      return "redirect:/admin/projects";
    } catch (Exception e) {
      log.warn("Saving a project request failed: {}", e.getMessage());
      String msg = e instanceof ResponseStatusException && ((ResponseStatusException) e).getReason() != null
          ? ((ResponseStatusException) e).getReason() : rootMessage(e);
      ProjectStaging current = s;
      if (current == null && stagingId != null) {
        try { current = workflow.get(stagingId); } catch (Exception ignored) { /* shown without a request */ }
      }
      return form(model, auth, form, current, msg);
    }
  }

  @PostMapping("/projects/staging/{stagingId}/photos/{imageId}/remove")
  public String removePhoto(@PathVariable Long stagingId, @PathVariable Long imageId, Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      workflow.removePhoto(stagingId, imageId, user);
      ra.addFlashAttribute("message", "Photo removed. A photo that is already live disappears only after the checker approves.");
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", e.getReason());
    }
    return "redirect:/admin/projects/staging/" + stagingId + "/edit";
  }

  // ================================================================================ maker: quick requests

  @PostMapping("/projects/{projectId}/reduce-units")
  public String reduceUnits(@PathVariable String projectId, @RequestParam int units, @RequestParam String reason,
                            Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      workflow.requestUnitReduction(projectId, units, reason, user);
      ra.addFlashAttribute("message", "Unit reduction sent to the checker. The live count changes only after approval.");
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", "Unit reduction failed: " + e.getReason());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("/projects/{projectId}/delete-request")
  public String requestDelete(@PathVariable String projectId, @RequestParam String reason, Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      workflow.requestDelete(projectId, reason, user);
      ra.addFlashAttribute("message", "Deletion sent to the checker. The project stays live until it is approved.");
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", "Deletion request failed: " + e.getReason());
    }
    return "redirect:/admin/projects";
  }

  /** Sends a saved draft (or a returned request) to the checker straight from the dashboard. */
  @PostMapping("/projects/staging/{stagingId}/submit")
  public String submit(@PathVariable Long stagingId, Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      workflow.submit(stagingId, user);
      ra.addFlashAttribute("message", "Sent to the checker for approval.");
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", "Could not submit: " + e.getReason());
    }
    return "redirect:/admin/projects";
  }

  @PostMapping("/projects/staging/{stagingId}/withdraw")
  public String withdraw(@PathVariable Long stagingId, Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.MAKERS);
    try {
      workflow.withdraw(stagingId, user);
      ra.addFlashAttribute("message", "Request withdrawn. Nothing live was changed.");
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", e.getReason());
    }
    return "redirect:/admin/projects";
  }

  // ================================================================================ checker

  @GetMapping("/projects/staging/{stagingId}/review")
  public String review(@PathVariable Long stagingId, Authentication auth, Model model, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.ALL_STAFF);
    ProjectStaging s = workflow.get(stagingId);
    if (!ProjectStaging.PENDING.equals(s.status)) {
      ra.addFlashAttribute("error", "This request is not waiting for approval (" + s.status + ").");
      return "redirect:/admin/projects";
    }
    try {
      workflow.requireCanView(s, user);
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", e.getReason());
      return "redirect:/admin/projects";
    }
    boolean create = ProjectStaging.CREATE.equals(s.requestType);
    boolean delete = ProjectStaging.DELETE.equals(s.requestType);
    Project live = create ? null : projectService.findById(s.projectId);
    AppUser me = CurrentUser.find(user);
    boolean checkerRole = me != null && ("ZO_CHECKER".equals(me.role) || "HO_MARKETING".equals(me.role) || "SUPERADMIN_DIT".equals(me.role));

    model.addAttribute("staging", s);
    model.addAttribute("project", live != null ? live : workflow.previewOf(s));
    model.addAttribute("isCreate", create);
    model.addAttribute("isDelete", delete);
    model.addAttribute("diff", ProjectStaging.UPDATE.equals(s.requestType) ? workflow.diff(live, s) : Collections.emptyList());
    model.addAttribute("contents", create ? workflow.contents(s) : Collections.emptyList());
    model.addAttribute("removedImages", workflow.removedImages(s));
    model.addAttribute("newImages", workflow.newImages(s));
    model.addAttribute("canDecide", checkerRole && !user.equals(s.createdBy));
    model.addAttribute("isOwner", user.equals(s.createdBy));
    return "change-review";
  }

  @PostMapping("/projects/staging/{stagingId}/decide")
  public String decide(@PathVariable Long stagingId, @RequestParam String decision,
                       @RequestParam(required = false) String comment, Authentication auth, RedirectAttributes ra) {
    String user = RoleCheck.require(auth, Roles.CHECKERS);
    try {
      ProjectStaging s = workflow.decide(stagingId, decision, comment, user);
      String what = ProjectStaging.CREATE.equals(s.requestType) ? "Project" : ProjectStaging.DELETE.equals(s.requestType) ? "Deletion" : "Change";
      ra.addFlashAttribute("message", what + " " + (ProjectStaging.APPROVED.equals(s.status) ? "approved." :
          ProjectStaging.REJECTED.equals(s.status) ? "rejected." : "returned to the maker."));
    } catch (ResponseStatusException e) {
      ra.addFlashAttribute("error", "Decision failed: " + e.getReason());
    }
    return "redirect:/admin/projects";
  }

  /** The request as a customer would see it (before it is approved), for the maker and for the checker. */
  @GetMapping("/projects/staging/{stagingId}/preview")
  public String preview(@PathVariable Long stagingId, Authentication auth, Model model) {
    String user = RoleCheck.require(auth, Roles.ALL_STAFF);
    ProjectStaging s = workflow.get(stagingId);
    workflow.requireCanView(s, user);
    model.addAttribute("project", workflow.previewOf(s));
    model.addAttribute("images", workflow.photosForForm(s));
    model.addAttribute("isStaffPreview", true);
    model.addAttribute("stagingPreview", true);
    model.addAttribute("stagingId", s.id);
    return "public-detail";
  }

  // ================================================================================ helpers

  private String form(Model model, Authentication auth, ProjectData shown, ProjectStaging s, String error) {
    AppUser user = CurrentUser.of(auth);
    if (user != null && "ZO_MAKER".equals(user.role) && user.zone != null) model.addAttribute("userZone", user.zone.trim());
    List<ProjectImage> photos = s != null ? workflow.photosForForm(s) : Collections.emptyList();
    if (error != null) model.addAttribute("error", error);
    model.addAttribute("project", shown);
    model.addAttribute("staging", s);
    model.addAttribute("changeMode", s != null && ProjectStaging.UPDATE.equals(s.requestType));
    model.addAttribute("zones", masterData.getZones());
    model.addAttribute("states", masterData.getStates());
    model.addAttribute("existingImages", photos);
    model.addAttribute("existingCount", photos.size());
    model.addAttribute("initialVisible", Math.max(photos.size(), 4));
    return "project-form";
  }

  private void addPhotos(ProjectStaging s, String username, MultipartFile... files) throws IOException {
    List<MultipartFile> incoming = new ArrayList<>();
    for (MultipartFile f : files) if (f != null && !f.isEmpty()) incoming.add(f);
    int count = workflow.photoCount(s);
    for (MultipartFile file : incoming) {
      if (count >= ProjectWorkflowService.MAX_PHOTOS) break;
      if (file.getSize() > MAX_PHOTO_BYTES) continue;
      String name = file.getOriginalFilename();
      String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : ".jpg";
      if (!Arrays.asList(".jpg", ".jpeg", ".png", ".webp").contains(ext)) continue;
      byte[] bytes = file.getBytes();
      String mime = PhotoFiles.sniffMime(bytes);
      if (mime == null || !PhotoFiles.extensionMatches(ext, mime)) continue;

      ProjectImage img = new ProjectImage();
      img.stagingId = s.id;                       // belongs to the maker's working copy: not live, never shown to customers
      img.projectId = null;
      img.fileName = PhotoFiles.cleanName(name != null ? name : "photo.jpg");
      img.mimeType = mime;
      img.fileSize = file.getSize();
      img.fileData = bytes;
      img.uploadedBy = username;
      img.isVerified = 0;
      img.filePath = "/files/pending/" + img.fileName;
      img = imageRepo.save(img);
      img.filePath = "/files/" + img.id;
      imageRepo.save(img);
      count++;
    }
  }

  private static String rootMessage(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
    return root.getMessage() != null && !root.getMessage().trim().isEmpty() ? root.getMessage() : e.getMessage();
  }
}
