package com.ucorealty.controller;

import com.ucorealty.model.Project;
import com.ucorealty.model.ProjectAttachment;
import com.ucorealty.model.ProjectImage;
import com.ucorealty.repository.ProjectAttachmentRepository;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.model.ProjectStaging;
import com.ucorealty.service.ProjectAccessService;
import com.ucorealty.service.ProjectWorkflowService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The project catalog for signed-in staff (/projects, /project/{id}) and every uploaded photo (/files/{id}) and PDF attachment (/attachments/{id}).
 * Only signed-in staff reach these pages (StaffSecurityConfig); staff see the live projects they are allowed to see; a maker's unapproved files are only handed to people who may open that request.
 * Change this file when: the catalog page, the project detail page or who may download a file changes.
 */
@Controller
public class ProjectCatalogController {
  private static final int PAGE_SIZE = 12;

  private final ProjectAccessService projectAccess;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;
  private final ProjectAttachmentRepository attachmentRepo;
  private final ProjectRepository projectRepo;
  private final ProjectWorkflowService workflow;

  private final com.ucorealty.service.ProjectHistoryService history;

  public ProjectCatalogController(ProjectAccessService projectAccess, MasterDataService masterDataService,
                             ProjectImageRepository imageRepo, ProjectAttachmentRepository attachmentRepo,
                             ProjectRepository projectRepo, ProjectWorkflowService workflow,
                             com.ucorealty.service.ProjectHistoryService history) {
    this.history = history;
    this.projectAccess = projectAccess;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
    this.attachmentRepo = attachmentRepo;
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
      list = ProjectAccessService.filter(projectAccess.approvedList(auth.getName()), state, city, search);
    } else {
      list = projectAccess.publicList(state, city, search);
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
    if (isStaff) {
      for (ProjectRepository.Place place : projectRepo.findPlaces()) {
        addCity(cities, state, place.getStateName(), place.getCityName());
      }
    } else {
      for (Project p : projectAccess.publicList(null, null, null)) {
        addCity(cities, state, p.stateName, p.cityName);
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

    Project p = isStaff ? projectAccess.findById(id) : projectAccess.publicById(id);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published");
    }

    List<ProjectImageRepository.FileInfo> imgs = imageRepo.findFilesOfProject(id);
    model.addAttribute("attachments", sortedDocuments(attachmentRepo.findFilesOfProject(id)));
    model.addAttribute("amenityRows", com.ucorealty.util.Amenities.parse(p.amenityFlags));
    model.addAttribute("amenityIcons", com.ucorealty.util.Amenities.ICONS);
    model.addAttribute("amenityDefaultIcon", com.ucorealty.util.Amenities.DEFAULT_ICON);
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    model.addAttribute("isStaffPreview", isStaff);
    if (isStaff) model.addAttribute("history", history.of(id));
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
      Project p = projectAccess.publicById(img.projectId);
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
    headers.setCacheControl("max-age=86400, private");   // private: a maker's unapproved file must never be kept by a shared proxy
    return new ResponseEntity<>(img.fileData, headers, HttpStatus.OK);
  }

  /** One PDF attachment. Same access rule as a photo: customers see only live ones of published projects; an unapproved one only people who may open that request. */
  @GetMapping("/attachments/{id}")
  public ResponseEntity<byte[]> getAttachment(@PathVariable Long id, Authentication auth) {
    ProjectAttachment att = attachmentRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    boolean isStaff = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()));
    if (!isStaff) {
      if (att.projectId == null || projectAccess.publicById(att.projectId) == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
    } else if (att.stagingId != null) {
      ProjectStaging request = workflow.get(att.stagingId);
      workflow.requireCanView(request, auth.getName());
    }
    if (att.fileData == null || att.fileData.length == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(ContentDisposition.inline().filename(att.fileName, StandardCharsets.UTF_8).build());   // "Save as" keeps the real name
    headers.setContentLength(att.fileData.length);
    headers.setCacheControl("max-age=86400, private");   // private: a maker's unapproved file must never be kept by a shared proxy
    return new ResponseEntity<>(att.fileData, headers, HttpStatus.OK);
  }

  /** Adds a city to the drop-down when it has a name and belongs to the chosen state (no state chosen = every state). */
  private static void addCity(Set<String> cities, String chosenState, String stateOfProject, String city) {
    if (city == null || city.trim().isEmpty()) return;
    if (chosenState == null || chosenState.isEmpty() || chosenState.equalsIgnoreCase(stateOfProject)) {
      cities.add(city.trim());
    }
  }

  /** The documents in display order: the named slots first, then the Others (oldest first inside each group). */
  private static java.util.List<ProjectAttachmentRepository.FileInfo> sortedDocuments(java.util.List<ProjectAttachmentRepository.FileInfo> files) {
    java.util.List<ProjectAttachmentRepository.FileInfo> sorted = new java.util.ArrayList<>(files);
    sorted.sort(java.util.Comparator.comparingInt((ProjectAttachmentRepository.FileInfo f) -> com.ucorealty.util.DocTypes.order(f.getDocType())).thenComparing(ProjectAttachmentRepository.FileInfo::getId));
    return sorted;
  }
}
