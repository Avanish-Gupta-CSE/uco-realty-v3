package com.ucorealty.internal.controller;

import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.internal.security.RoleCheck;
import com.ucorealty.service.ProjectService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Project photos, stored directly as Oracle SecureFiles BLOBs.
 */
@RestController
public class FileController {
  static final int MAX_FILES_PER_PROJECT = 10;
  static final long MAX_BYTES = 2L * 1024 * 1024;
  static final String[] MAKERS = {"ZO_MAKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  static final String[] ALL_STAFF = {
    "SUPERADMIN_DIT", "HO_ADMIN", "HO_MARKETING", "HO_RETAIL",
    "ZO_MAKER", "ZO_CHECKER", "CALL_CENTRE"
  };
  private static final Set<String> SERVABLE = new HashSet<>(Arrays.asList("image/png", "image/jpeg", "image/webp"));

  private final ProjectImageRepository images;
  private final ProjectRepository projects;
  private final ProjectService projectService;

  public FileController(ProjectImageRepository images, ProjectRepository projects, ProjectService projectService) {
    this.images = images;
    this.projects = projects;
    this.projectService = projectService;
  }

  /** Real file type from the first bytes, or null if it is not an allowed type. */
  static String sniffMime(byte[] b) {
    if (b == null || b.length < 12) return null;
    if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
    if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
    if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
      return "image/webp";
    return null;
  }

  static boolean extensionMatches(String ext, String mime) {
    if (".png".equals(ext)) return "image/png".equals(mime);
    if (".jpg".equals(ext) || ".jpeg".equals(ext)) return "image/jpeg".equals(mime);
    if (".webp".equals(ext)) return "image/webp".equals(mime);
    return false;
  }

  static String cleanName(String name) {
    return name.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  @PostMapping("/api/admin/projects/{id}/files")
  public List<Map<String, Object>> upload(@PathVariable String id,
                                          @RequestParam("files") List<MultipartFile> files,
                                          @RequestParam(defaultValue = "photo") String kind,
                                          Authentication auth) throws Exception {
    String user = RoleCheck.require(auth, MAKERS);
    Project p = projectService.requireEditableProject(id, user);

    if (files.size() > 10) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max 10 files per upload batch");
    }
    if (images.findByProjectId(id).size() + files.size() > MAX_FILES_PER_PROJECT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "A project can have at most " + MAX_FILES_PER_PROJECT + " photos");
    }

    List<Map<String, Object>> out = new ArrayList<>();
    for (MultipartFile f : files) {
      if (f.isEmpty()) continue;

      String originalFilename = f.getOriginalFilename() != null ? f.getOriginalFilename() : "document";
      String ext = originalFilename.contains(".")
        ? originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase() : "";
      if (!Arrays.asList(".png", ".jpg", ".jpeg", ".webp").contains(ext)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only photos are accepted (JPG, PNG or WEBP)");
      }
      if (f.getSize() > MAX_BYTES) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, cleanName(originalFilename) + " is larger than 2 MB. Please use a smaller photo.");
      }

      byte[] bytes = f.getBytes();
      String mime = sniffMime(bytes);
      if (mime == null || !extensionMatches(ext, mime)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          cleanName(originalFilename) + " is not a valid " + ext.substring(1).toUpperCase() + " file");
      }

      String safeName = cleanName(originalFilename);
      ProjectImage img = new ProjectImage();
      img.projectId = id;
      img.fileName = safeName;
      img.mimeType = mime;
      img.fileSize = f.getSize();
      img.fileData = bytes;
      img.uploadedBy = user;
      img.filePath = "/api/public/files/pending/" + safeName;
      img = images.save(img);
      img.filePath = "/api/public/files/" + img.id + "/" + safeName;
      images.save(img);

      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", img.id);
      map.put("url", img.filePath);
      map.put("fileName", img.fileName);
      map.put("size", img.fileSize);
      out.add(map);
    }

    p.totalImgCount = images.findByProjectId(id).size();
    projects.save(p);
    return out;
  }

  /** Public or staff photo serving */
  @GetMapping(value = {"/files/{id}", "/files/{id}/{name}", "/api/public/files/{id}", "/api/public/files/{id}/{name}"})
  public ResponseEntity<byte[]> serveBlob(@PathVariable Long id,
                                          @PathVariable(required = false) String name,
                                          Authentication auth) {
    ProjectImage img = images.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
      projectService.publicById(img.projectId); // 404 unless the project is public
    }
    return blob(img, "public, max-age=3600");
  }

  /** Staff: any file, any status (checkers must see photos before approving). Login required. */
  @GetMapping(value = {"/api/admin/files/{id}", "/api/admin/files/{id}/{name}"})
  public ResponseEntity<byte[]> serveBlobToStaff(@PathVariable Long id,
                                                 @PathVariable(required = false) String name,
                                                 Authentication auth) {
    RoleCheck.require(auth, ALL_STAFF);
    ProjectImage img = images.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    return blob(img, "private, no-store");
  }

  private ResponseEntity<byte[]> blob(ProjectImage img, String cacheControl) {
    byte[] data = img.fileData;
    if (data == null || data.length == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content is empty");
    }
    String mime = SERVABLE.contains(img.mimeType) ? img.mimeType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    String safe = img.fileName == null ? "file" : cleanName(img.fileName);
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(mime))
      .contentLength(data.length)
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe + "\"")
      .header("X-Content-Type-Options", "nosniff")
      .header(HttpHeaders.CACHE_CONTROL, cacheControl)
      .body(data);
  }

  @DeleteMapping("/api/admin/projects/{id}/files/{fileId}")
  public Map<String, Object> deleteFile(@PathVariable String id,
                                        @PathVariable Long fileId,
                                        Authentication auth) {
    String user = RoleCheck.require(auth, MAKERS);
    Project p = projectService.requireEditableProject(id, user);

    ProjectImage img = images.findById(fileId)
      .filter(i -> i.projectId.equals(id))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    images.delete(img);

    p.totalImgCount = images.findByProjectId(id).size();
    projects.save(p);
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("success", true);
    map.put("deletedId", fileId);
    map.put("remainingCount", p.totalImgCount);
    return map;
  }

  /** Public list of photos for a project that is public. */
  @GetMapping("/api/public/projects/{id}/images")
  public List<Map<String, Object>> list(@PathVariable String id) {
    projectService.publicById(id);
    return images.findByProjectId(id).stream()
      .filter(i -> i.mimeType != null && i.mimeType.startsWith("image/"))
      .map(i -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.id);
        m.put("url", i.filePath);
        m.put("fileName", i.fileName != null ? i.fileName : "file");
        m.put("verified", i.isVerified);
        m.put("size", i.fileSize);
        return m;
      }).collect(Collectors.toList());
  }
}
