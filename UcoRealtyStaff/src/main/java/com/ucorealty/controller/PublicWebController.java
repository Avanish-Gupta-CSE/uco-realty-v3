package com.ucorealty.controller;

import com.ucorealty.model.Project;
import com.ucorealty.model.ProjectImage;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.model.ProjectStaging;
import com.ucorealty.service.ProjectService;
import com.ucorealty.service.ProjectWorkflowService;
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
  private static final int PAGE_SIZE = 12;

  private final ProjectService projectService;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;
  private final ProjectRepository projectRepo;
  private final ProjectWorkflowService workflow;

  public PublicWebController(ProjectService projectService, MasterDataService masterDataService,
                             ProjectImageRepository imageRepo, ProjectRepository projectRepo,
                             ProjectWorkflowService workflow) {
    this.projectService = projectService;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
    this.projectRepo = projectRepo;
    this.workflow = workflow;
  }

  @GetMapping("/projects")
  public String listProjects(@RequestParam(required = false) String state,
                             @RequestParam(required = false) String city,
                             @RequestParam(required = false) String search,
                             @RequestParam(defaultValue = "1") int pageNo,
                             Authentication auth,
                             Model model) {
    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));

    List<Project> list;
    if (isStaff) {
      // Authenticated bank staff on the intranet see the live (approved) projects they are allowed to see
      list = projectService.approvedList(auth.getName());
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

    // 12 cards per page
    int total = list.size();
    int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    int current = Math.min(Math.max(1, pageNo), pageCount);
    List<Project> pageItems = list.subList((current - 1) * PAGE_SIZE, Math.min(current * PAGE_SIZE, total));
    model.addAttribute("projects", pageItems);
    model.addAttribute("projectCount", total);
    model.addAttribute("pageCount", pageCount);
    model.addAttribute("pageNo", current);
    model.addAttribute("states", masterDataService.getStates());
    model.addAttribute("isStaff", isStaff);

    Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    List<Project> allProjects = isStaff ? projectRepo.findAll() : projectService.publicList(null, null, null);
    for (Project p : allProjects) {
      if ("DELETED".equals(p.status)) continue;
      if (p.cityName != null && !p.cityName.trim().isEmpty()) {
        if (state == null || state.isEmpty() || state.equalsIgnoreCase(p.stateName)) {
          cities.add(p.cityName.trim());
        }
      }
    }
    model.addAttribute("cities", cities);

    Map<String, Long> thumbnails = new HashMap<>();
    if (!pageItems.isEmpty()) {
      for (ProjectImageRepository.FirstPhoto photo : imageRepo.findFirstImageIds(pageItems.stream().map(p -> p.id).collect(Collectors.toList()))) {
        thumbnails.put(photo.getProjectId(), photo.getImageId());
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

    List<ProjectImage> all = imageRepo.findByProjectId(id);
    List<ProjectImage> imgs = all.stream().filter(ProjectImage::isPhoto).collect(Collectors.toList());
    ProjectWorkflowController.addDocumentLinks(model, all.stream().filter(i -> !i.isPhoto()).collect(Collectors.toList()));
    model.addAttribute("amenityRows", com.ucorealty.util.Amenities.parse(p.amenityFlags));
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    model.addAttribute("isStaffPreview", isStaff);
    return "public-detail";
  }

  @GetMapping("/files/{id}")
  public ResponseEntity<byte[]> getFile(@PathVariable Long id, Authentication auth) {
    ProjectImage img = imageRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));
    if (!isStaff) {
      if (img.projectId == null) {                      // belongs to a maker's unapproved work: customers must not see it
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      Project p = projectService.publicById(img.projectId);
      if (p == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
    } else if (img.stagingId != null) {
      // A file of a maker's unapproved work: only people who may open that request (the maker, or the checker of the zone) may fetch it.
      // The same check protects the edit, review and preview pages of the request.
      ProjectStaging request = workflow.get(img.stagingId);
      workflow.requireCanView(request, auth.getName());
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
