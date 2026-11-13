package com.ucorealty.controller;

import com.ucorealty.model.Project;
import com.ucorealty.model.ProjectImage;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.ProjectService;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * The customer pages: the catalog (/projects), one project (/project/{id}) and the files (/files/{id}: photos and PDFs).
 * Only approved, active and published projects are shown. Change this file when: a customer page needs other data.
 */
@Controller
public class CatalogPageController {
  private static final int PAGE_SIZE = 12;

  private final ProjectService projectService;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;

  /** Where "Apply for Home Loan" sends the customer: bank site (uco.homeloan.base-url, differs per environment) + this fixed path. */
  private static final String HOME_LOAN_PATH = "/Lead_App/lead_web.jsp?utm=REALTY";
  private final String homeLoanUrl;

  public CatalogPageController(ProjectService projectService, MasterDataService masterDataService,
                             ProjectImageRepository imageRepo,
                             @Value("${uco.homeloan.base-url}") String homeLoanBaseUrl) {
    this.homeLoanUrl = homeLoanBaseUrl.replaceAll("/+$", "") + HOME_LOAN_PATH;
    this.projectService = projectService;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
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
      for (ProjectImageRepository.FirstPhoto photo : imageRepo.findFirstImageIds(pageItems.stream().map(p -> p.id).collect(Collectors.toList()))) {
        thumbnails.put(photo.getProjectId(), photo.getImageId());
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

    List<Long> imgs = new ArrayList<>();
    for (ProjectImageRepository.PhotoId photo : imageRepo.findPhotoIds(id)) {
      imgs.add(photo.getImageId());
    }
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    for (ProjectImageRepository.DocumentInfo doc : imageRepo.findDocuments(id)) {      // PDF documents
      model.addAttribute(ProjectImage.BROCHURE.equals(doc.getFileKind()) ? "brochureId" : "floorPlanId", doc.getImageId());
    }
    model.addAttribute("amenityRows", com.ucorealty.util.Amenities.parse(p.amenityFlags));
    model.addAttribute("homeLoanUrl", homeLoanUrl);
    return "public-detail";
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

