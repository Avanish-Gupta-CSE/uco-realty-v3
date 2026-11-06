package com.ucorealty.controller;

import com.ucorealty.entity.Lead;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.service.LeadService;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.ProjectService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PublicWebController {
  private static final int PAGE_SIZE = 12;

  private final ProjectService projectService;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;
  private final LeadService leadService;

  public PublicWebController(ProjectService projectService, MasterDataService masterDataService,
                             ProjectImageRepository imageRepo, LeadService leadService) {
    this.projectService = projectService;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
    this.leadService = leadService;
  }

  @GetMapping({"/", "/projects"})
  public String listProjects(@RequestParam(required = false) String state,
                             @RequestParam(required = false) String city,
                             @RequestParam(required = false) String search,
                             @RequestParam(defaultValue = "1") int pageNo,
                             Model model) {
    List<Project> list = projectService.publicList(state, city, search);

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

    Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (Project p : projectService.publicList(null, null, null)) {
      if (p.cityName != null && !p.cityName.trim().isEmpty()) {
        if (state == null || state.isEmpty() || state.equalsIgnoreCase(p.stateName)) {
          cities.add(p.cityName.trim());
        }
      }
    }
    model.addAttribute("cities", cities);

    Map<String, Long> thumbnails = new HashMap<>();
    if (!pageItems.isEmpty()) {
      for (Object[] row : imageRepo.findFirstImageIds(pageItems.stream().map(p -> p.id).collect(Collectors.toList()))) {
        thumbnails.put((String) row[1], (Long) row[0]);
      }
    }
    model.addAttribute("projectThumbnails", thumbnails);

    return "public-projects";
  }

  @GetMapping("/project/{id}")
  public String projectDetail(@PathVariable String id, Model model) {
    Project p = projectService.publicById(id);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published");
    }

    List<Long> imgs = imageRepo.findIdsByProjectId(id);
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    return "public-detail";
  }

  @PostMapping("/lead")
  public Object createLead(@RequestParam String name,
                           @RequestParam String phone,
                           @RequestParam(required = false) String projectId,
                           @RequestParam(required = false) String branchSelected,
                           @RequestParam(defaultValue = LeadService.STATUS_INTEREST) String status,
                           @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
    boolean ajax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    Lead l;
    try {
      l = leadService.capture(name, phone, projectId, branchSelected, status);
    } catch (ResponseStatusException e) {
      if (ajax) {
        return ResponseEntity.status(e.getStatusCode()).body(Collections.singletonMap("error", e.getReason()));
      }
      return "redirect:/projects?error=invalid_lead";
    }

    if (ajax) {
      Map<String, Object> resp = new HashMap<>();
      resp.put("success", true);
      resp.put("leadId", l.id);
      resp.put("status", l.status);
      return ResponseEntity.ok(resp);
    }

    if (LeadService.STATUS_INTEREST.equals(l.status)) {
      return "redirect:https://www.uco.bank.in/home-loan";
    }
    if (l.projectId != null) {
      return "redirect:/project/" + l.projectId;
    }
    return "redirect:/projects";
  }

  @GetMapping("/files/{id}")
  public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
    ProjectImage img = imageRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (img.projectId == null) {                      // belongs to a maker's unapproved work: not live
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Project p = projectService.publicById(img.projectId);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    if (img.fileData == null || img.fileData.length == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    if (img.mimeType != null) {
      try {
        mediaType = MediaType.parseMediaType(img.mimeType);
      } catch (Exception ignored) {}
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
        .body(img.fileData);
  }
}

