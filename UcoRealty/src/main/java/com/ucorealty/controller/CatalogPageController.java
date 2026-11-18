package com.ucorealty.controller;

import com.ucorealty.model.Project;
import com.ucorealty.model.ProjectAttachment;
import com.ucorealty.model.ProjectImage;
import com.ucorealty.repository.ProjectAttachmentRepository;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.service.MasterDataService;
import com.ucorealty.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The customer pages: the catalog (/projects), one project (/project/{id}) and
 * the files (/files/{id}: photos, /attachments/{id}: PDF attachments).
 * Only approved, active and published projects are shown. Change this file
 * when: a customer page needs other data.
 */
@Controller
public class CatalogPageController {
  private static final int PAGE_SIZE = 12;

  private final CatalogService catalogService;
  private final MasterDataService masterDataService;
  private final ProjectImageRepository imageRepo;
  private final ProjectAttachmentRepository attachmentRepo;

  /**
   * Where "Apply for Home Loan" sends the customer: bank site
   * (uco.homeloan.base-url, differs per environment) + this fixed path.
   */
  private static final String HOME_LOAN_PATH = "/Lead_App/lead_web.jsp?utm=REALTY";
  private final String homeLoanUrl;

  public CatalogPageController(CatalogService catalogService, MasterDataService masterDataService,
      ProjectImageRepository imageRepo, ProjectAttachmentRepository attachmentRepo,
      @Value("${uco.homeloan.base-url}") String homeLoanBaseUrl) {
    this.homeLoanUrl = homeLoanBaseUrl.replaceAll("/+$", "") + HOME_LOAN_PATH;
    this.catalogService = catalogService;
    this.masterDataService = masterDataService;
    this.imageRepo = imageRepo;
    this.attachmentRepo = attachmentRepo;
  }

  @GetMapping({ "/", "/projects" })
  public String listProjects(@RequestParam(required = false) String state,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "1") int pageNo,
      Model model) {
    List<Project> allPublic = catalogService.allPublic(); // read once: the list and the city drop-down both come from
                                                          // it
    List<Project> list = CatalogService.filter(allPublic, state, city, search);

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
    for (Project p : allPublic) {
      if (p.cityName != null && !p.cityName.trim().isEmpty()) {
        if (state == null || state.isEmpty() || state.equalsIgnoreCase(p.stateName)) {
          cities.add(p.cityName.trim());
        }
      }
    }
    model.addAttribute("cities", cities);

    Map<String, Long> thumbnails = new HashMap<>();
    if (!pageItems.isEmpty()) {
      for (ProjectImageRepository.FirstPhoto photo : imageRepo
          .findFirstImageIds(pageItems.stream().map(p -> p.id).collect(Collectors.toList()))) {
        thumbnails.put(photo.getProjectId(), photo.getImageId());
      }
    }
    model.addAttribute("projectThumbnails", thumbnails);

    return "public-projects";
  }

  @GetMapping("/project/{id}")
  public String projectDetail(@PathVariable String id, Model model) {
    Project p = catalogService.publicById(id);
    if (p == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published");
    }

    List<Long> imgs = new ArrayList<>();
    for (ProjectImageRepository.PhotoId photo : imageRepo.findPhotoIds(id)) {
      imgs.add(photo.getImageId());
    }
    model.addAttribute("project", p);
    model.addAttribute("images", imgs);
    model.addAttribute("attachments", attachmentRepo.findFilesOfProject(id));   // PDF attachments: any number
    model.addAttribute("amenityRows", com.ucorealty.util.Amenities.parse(p.amenityFlags));
    model.addAttribute("homeLoanUrl", homeLoanUrl);
    return "public-detail";
  }

  @GetMapping("/files/{id}")
  public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
    ProjectImage img = imageRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (img.projectId == null) { // belongs to a maker's unapproved work: not live
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Project p = catalogService.publicById(img.projectId);
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
      } catch (Exception ignored) {
      }
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
        .body(img.fileData);
  }

  /** One PDF attachment of a live project. A maker's unapproved attachment (no project id) and one of an unpublished project give 404. */
  @GetMapping("/attachments/{id}")
  public ResponseEntity<byte[]> getAttachment(@PathVariable Long id) {
    ProjectAttachment att = attachmentRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (att.projectId == null || catalogService.publicById(att.projectId) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    if (att.fileData == null || att.fileData.length == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(att.fileName, StandardCharsets.UTF_8).build().toString())   // "Save as" keeps the real name
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
        .body(att.fileData);
  }
}
