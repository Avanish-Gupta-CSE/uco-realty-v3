package com.ucorealty.controller;

import com.ucorealty.dto.InventoryReduceRequest;
import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.Project;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.security.RoleCheck;
import com.ucorealty.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {
  static final String[] MAKERS = {"ZO_MAKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  static final String[] CHECKERS = {"ZO_CHECKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  static final String[] ALL_STAFF = {
    "SUPERADMIN_DIT", "HO_ADMIN", "HO_MARKETING", "HO_RETAIL",
    "ZO_MAKER", "ZO_CHECKER", "CALL_CENTRE"
  };

  private final ProjectService service;
  private final ProjectImageRepository imageRepo;

  public AdminProjectController(ProjectService service, ProjectImageRepository imageRepo) {
    this.service = service;
    this.imageRepo = imageRepo;
  }

  @GetMapping
  public List<Project> list(@RequestParam(required = false, defaultValue = "jurisdiction") String scope,
                            Authentication auth) {
    String username = RoleCheck.require(auth, ALL_STAFF);
    return service.adminList(username, scope);
  }

  @GetMapping("/{id}")
  public Map<String, Object> getProject(@PathVariable String id, Authentication auth) {
    RoleCheck.require(auth, ALL_STAFF);
    Project p = service.findById(id);
    Map<String, Object> res = new LinkedHashMap<>();
    res.put("project", p);
    // Staff-only URLs: checkers must see photos of projects that are not public yet.
    res.put("images", imageRepo.findByProjectId(id).stream()
      .filter(i -> i.mimeType != null && i.mimeType.startsWith("image/"))
      .map(i -> i.filePath == null ? null : i.filePath.replace("/api/public/files/", "/api/admin/files/"))
      .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList()));
    res.put("auditLogs", service.getAuditLogs(id));
    return res;
  }

  @GetMapping("/{id}/audit")
  public List<AuditLog> getAudit(@PathVariable String id, Authentication auth) {
    RoleCheck.require(auth, ALL_STAFF);
    return service.getAuditLogs(id);
  }

  @PostMapping
  public Project create(@RequestBody Project p, Authentication auth) {
    String user = RoleCheck.require(auth, MAKERS);
    if ("PENDING_APPROVAL".equals(p.status)) {
      Project s = service.saveDraft(p, user);
      return service.submit(s.id, user);
    }
    return service.saveDraft(p, user);
  }

  @PutMapping("/{id}")
  public Project update(@PathVariable String id, @RequestBody Project p, Authentication auth) {
    String user = RoleCheck.require(auth, MAKERS);
    return service.updateDraft(id, p, user);
  }

  @PostMapping("/{id}/submit")
  public Project submit(@PathVariable String id, Authentication auth) {
    String user = RoleCheck.require(auth, MAKERS);
    return service.submit(id, user);
  }

  // HO Retail lowers the available units of an approved project (their discretion, reason required, audited).
  @PostMapping("/{id}/inventory/reduce")
  public Project reduceInventory(@PathVariable String id, @Valid @RequestBody InventoryReduceRequest req,
                                 Authentication auth) {
    String user = RoleCheck.require(auth, "HO_RETAIL");
    return service.reduceInventory(id, req.units, req.reason, user);
  }

  @PostMapping("/{id}/{decision}")
  public Project decide(@PathVariable String id, @PathVariable String decision,
                        @RequestBody(required = false) Map<String, String> body,
                        Authentication auth) {
    String checker = RoleCheck.require(auth, CHECKERS);
    String comment = body != null ? body.getOrDefault("comment", "") : "";
    return service.decide(id, decision, checker, comment);
  }
}
