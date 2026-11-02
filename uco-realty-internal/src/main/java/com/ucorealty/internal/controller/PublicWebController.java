package com.ucorealty.internal.controller;

import com.ucorealty.entity.Lead;
import com.ucorealty.entity.LeadTransferLog;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.LeadTransferLogRepository;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intranet Project Catalog & Customer Lead Controller.
 * Provides end-to-end catalog browsing on the bank corporate intranet.
 * For authenticated bank staff, displays all projects across the bank.
 */
@Controller
public class PublicWebController {

  private final ProjectService projectService;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;
  private final ProjectRepository projectRepo;
  private final LeadRepository leadRepo;
  private final LeadTransferLogRepository transferRepo;

  public PublicWebController(ProjectService projectService, MasterDataService masterDataService,
                             ProjectImageRepository imageRepo, ProjectRepository projectRepo,
                             LeadRepository leadRepo, LeadTransferLogRepository transferRepo) {
    this.projectService = projectService;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
    this.projectRepo = projectRepo;
    this.leadRepo = leadRepo;
    this.transferRepo = transferRepo;
  }

  @GetMapping("/projects")
  public String listProjects(@RequestParam(required = false) String state,
                             @RequestParam(required = false) String city,
                             @RequestParam(required = false) String search,
                             Authentication auth,
                             Model model) {
    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));

    List<Project> list;
    if (isStaff) {
      // Authenticated Bank Staff on Intranet sees everything
      list = projectService.adminList(auth.getName(), "all");
      if (state != null && !state.trim().isEmpty()) {
        list = list.stream().filter(p -> state.equalsIgnoreCase(p.stateName)).collect(Collectors.toList());
      }
      if (city != null && !city.trim().isEmpty()) {
        list = list.stream().filter(p -> city.equalsIgnoreCase(p.cityName)).collect(Collectors.toList());
      }
      if (search != null && !search.trim().isEmpty()) {
        String s = search.toLowerCase();
        list = list.stream().filter(p ->
            (p.projectName != null && p.projectName.toLowerCase().contains(s)) ||
            (p.builderName != null && p.builderName.toLowerCase().contains(s)) ||
            (p.pinCode != null && p.pinCode.contains(s)) ||
            (p.reraNo != null && p.reraNo.toLowerCase().contains(s))
        ).collect(Collectors.toList());
      }
    } else {
      list = projectService.publicList(state, city, search);
    }

    model.addAttribute("projects", list);
    model.addAttribute("states", masterDataService.getStates());
    model.addAttribute("isStaff", isStaff);

    Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    List<Project> allProjects = isStaff ? projectRepo.findAll() : projectService.publicList(null, null, null);
    for (Project p : allProjects) {
      if (p.cityName != null && !p.cityName.trim().isEmpty()) {
        if (state == null || state.isEmpty() || state.equalsIgnoreCase(p.stateName)) {
          cities.add(p.cityName.trim());
        }
      }
    }
    model.addAttribute("cities", cities);

    Map<String, Long> thumbnails = new HashMap<>();
    for (Project p : list) {
      List<ProjectImage> imgs = imageRepo.findByProjectId(p.id);
      if (!imgs.isEmpty()) {
        thumbnails.put(p.id, imgs.get(0).id);
      }
    }
    model.addAttribute("projectThumbnails", thumbnails);

    return "public-projects";
  }

  @GetMapping("/project/{id}")
  public String projectDetail(@PathVariable String id, Authentication auth, Model model) {
    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));

    Project p = isStaff ? projectService.findById(id) : projectService.publicById(id);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published");
    }

    List<ProjectImage> imgs = imageRepo.findByProjectId(id);
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    model.addAttribute("isStaffPreview", isStaff);
    return "public-detail";
  }

  @PostMapping("/lead")
  public Object createLead(@RequestParam String name,
                           @RequestParam String phone,
                           @RequestParam(required = false) String projectId,
                           @RequestParam(required = false) String branchSelected,
                           @RequestParam(defaultValue = "Showed interest in Home Loan") String status,
                           @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
    if (name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
      if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Name and mobile are required"));
      }
      return "redirect:/projects?error=missing_info";
    }

    String normalizedStatus = "Showed interest in Home Loan";
    if ("VISITOR".equalsIgnoreCase(status) || "Visitor".equalsIgnoreCase(status)) {
      normalizedStatus = "Visitor";
    }

    Lead l = new Lead();
    l.customerName = name.trim();
    l.phone = phone.trim();
    l.projectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId.trim() : null;
    l.branchSelected = (branchSelected != null && !branchSelected.trim().isEmpty()) ? branchSelected.trim() : null;
    l.status = normalizedStatus;
    leadRepo.save(l);

    LeadTransferLog t = new LeadTransferLog();
    t.leadId = l.id;
    t.payload = "{\"name\":\"" + l.customerName + "\",\"phone\":\"" + l.phone + "\",\"project\":\"" + l.projectId + "\"}";
    transferRepo.save(t);

    if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
      Map<String, Object> resp = new HashMap<>();
      resp.put("success", true);
      resp.put("leadId", l.id);
      resp.put("status", l.status);
      return ResponseEntity.ok(resp);
    }

    if ("Showed interest in Home Loan".equals(normalizedStatus)) {
      return "redirect:https://www.uco.bank.in/home-loan";
    }

    if (l.projectId != null) {
      return "redirect:/project/" + l.projectId;
    }
    return "redirect:/projects";
  }

  @GetMapping("/files/{id}")
  public ResponseEntity<byte[]> getFile(@PathVariable Long id, Authentication auth) {
    ProjectImage img = imageRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));
    if (!isStaff) {
      Project p = projectService.publicById(img.projectId);
      if (p == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
    }

    if (img.fileData == null || img.fileData.length == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(img.mimeType != null ? img.mimeType : "image/jpeg"));
    headers.setContentLength(img.fileData.length);
    headers.setCacheControl("max-age=86400, public");
    return new ResponseEntity<>(img.fileData, headers, HttpStatus.OK);
  }
}
