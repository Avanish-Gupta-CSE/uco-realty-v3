package com.ucorealty.service;

import com.ucorealty.entity.AppUser;
import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectData;
import com.ucorealty.repository.AuditLogRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reading side of the MASTER table (approved projects) plus the rules shared by the maker-checker workflow.
 * Everything a maker creates or changes goes through ProjectWorkflowService (staging table) instead.
 */
@Service
public class ProjectService {
  private final ProjectRepository repo;
  private final AuditLogRepository audit;

  public ProjectService(ProjectRepository repo, AuditLogRepository audit) {
    this.repo = repo;
    this.audit = audit;
  }

  // ---- shared rules (package-private statics, used by the workflow service) ----

  /** The signed-in employee (role and zone were taken from HRMS at login and live in the session). */
  AppUser requireActiveUser(String username) {
    AppUser u = CurrentUser.find(username);
    if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again");
    return u;
  }

  static boolean sameText(String a, String b) {
    return a != null && b != null && !a.trim().isEmpty() && a.trim().equalsIgnoreCase(b.trim());
  }

  static boolean isNationalMaker(AppUser u) {
    return "SUPERADMIN_DIT".equals(u.role) || "HO_MARKETING".equals(u.role);
  }

  static boolean isNational(AppUser u) {
    return isNationalMaker(u) || "HO_RETAIL".equals(u.role);
  }

  // The screens hide buttons by role, but the rules must also hold when someone posts a form directly.
  static void requireMakerRole(AppUser u) {
    if (!"ZO_MAKER".equals(u.role) && !isNationalMaker(u))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot create or change projects");
  }

  static void requireCheckerRole(AppUser u) {
    if (!"ZO_CHECKER".equals(u.role) && !isNationalMaker(u))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a Zonal Checker (or Head Office Marketing / DIT) can approve, reject or return a request");
  }

  /** A zonal maker/checker works only inside their own zone; national roles in every zone. */
  static void requireZoneAccess(AppUser u, String zone) {
    if (isNational(u)) return;
    if (!sameText(u.zone, zone))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This project belongs to another zone (" + zone + ")");
  }

  static void requireComment(String comment) {
    if (comment == null || comment.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A remark is required to reject or return a request");
    }
  }

  /** Free text typed by staff (reasons, remarks): markup characters are refused. */
  static void requireNoMarkup(String what, String text) {
    if (text != null && (text.indexOf('<') >= 0 || text.indexOf('>') >= 0))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, what + " must not contain the characters < or >");
  }

  // Format checks on everything the maker typed (see ProjectValidator). The pincode is trimmed first.
  static void validateDetails(ProjectData p) {
    if (p.pinCode != null) p.pinCode = p.pinCode.trim();
    List<String> problems = ProjectValidator.problems(p);
    if (!problems.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please correct: " + String.join("; ", problems));
    }
  }

  // What a project must have before it can go to the checker (the mandatory BRD fields + pincode).
  static void requireSubmittable(ProjectData p) {
    List<String> missing = new ArrayList<>();
    if (blank(p.zoneName)) missing.add("Zone Name");
    if (blank(p.branchName)) missing.add("Branch Name");
    if (blank(p.stateName)) missing.add("State Name");
    if (blank(p.cityName)) missing.add("City Name");
    if (blank(p.projectName)) missing.add("Project Name");
    if (blank(p.reraNo)) missing.add("RERARegistrationNo");
    if (p.totalInventory == null || p.totalInventory < 1) missing.add("TotalInventory");
    if (blank(p.builderName)) missing.add("BuilderName");
    if (blank(p.projectAddress)) missing.add("ProjectAddress");
    if (blank(p.pinCode)) missing.add("Pincode");
    if (!missing.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please fill in before submitting: " + String.join(", ", missing));
    }
  }

  private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

  // ---- customers (only live, active, published projects) ----

  private static boolean isVisibleToPublic(Project p) {
    return Project.APPROVED.equals(p.status)
      && Integer.valueOf(1).equals(p.isActive)
      && Integer.valueOf(1).equals(p.isPublished);
  }

  public Project publicById(String id) {
    return repo.findById(id).filter(ProjectService::isVisibleToPublic)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published"));
  }

  public List<Project> publicList(String state, String city, String search) {
    List<Project> all = repo.findByStatusAndIsActiveAndIsPublished(Project.APPROVED, 1, 1);
    return all.stream()
      .filter(p -> state == null || state.trim().isEmpty() || state.equalsIgnoreCase(p.stateName))
      .filter(p -> city == null || city.trim().isEmpty() || city.equalsIgnoreCase(p.cityName))
      .filter(p -> search == null || search.trim().isEmpty() ||
          (p.projectName != null && p.projectName.toLowerCase().contains(search.toLowerCase())) ||
          (p.builderName != null && p.builderName.toLowerCase().contains(search.toLowerCase())) ||
          (p.pinCode != null && p.pinCode.contains(search)) ||
          (p.reraNo != null && p.reraNo.toLowerCase().contains(search.toLowerCase())))
      .collect(Collectors.toList());
  }

  // ---- staff ----

  /** Live (approved) projects the user may see: all zones for Head Office roles, own zone for zonal staff. DELETED never. */
  public List<Project> approvedList(String username) {
    AppUser user = requireActiveUser(username);
    List<Project> visible;
    if (isNational(user)) {
      visible = repo.findAll();
    } else if ("ZO_MAKER".equals(user.role) || "ZO_CHECKER".equals(user.role)) {
      visible = (user.zone == null || user.zone.trim().isEmpty()) ? Collections.emptyList() : repo.findByZoneNameIgnoreCase(user.zone.trim());
    } else {
      visible = Collections.emptyList();
    }
    return visible.stream().filter(p -> !Project.DELETED.equals(p.status)).collect(Collectors.toList());
  }

  /** A live project by code; a DELETED one behaves as if it did not exist. */
  public Project findById(String id) {
    return repo.findById(id).filter(p -> !Project.DELETED.equals(p.status)).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Project " + id + " not found"));
  }

  public List<AuditLog> getAuditLogs(String id) {
    return audit.findByEntityIdOrderByCreatedAtDesc(id);
  }
}
