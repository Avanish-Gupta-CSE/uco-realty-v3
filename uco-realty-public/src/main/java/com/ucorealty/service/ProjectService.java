package com.ucorealty.service;

import com.ucorealty.entity.AppUser;
import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.Project;
import com.ucorealty.repository.AuditLogRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectService {
  private final ProjectRepository repo;
  private final AuditLogRepository audit;
  private final UserRepository users;

  public ProjectService(ProjectRepository repo, AuditLogRepository audit, UserRepository users) {
    this.repo = repo;
    this.audit = audit;
    this.users = users;
  }

  private void log(String entityId, String action, String by, String oldVal, String newVal) {
    AuditLog a = new AuditLog();
    a.entityType = "PROJECT";
    a.entityId = entityId;
    a.action = action;
    a.byUser = by;
    a.oldVal = oldVal;
    a.newVal = newVal;
    audit.save(a);
  }

  // ---- shared rules -------------------------------------------------------

  private AppUser requireActiveUser(String username) {
    return users.findByUsername(username)
      .filter(u -> Integer.valueOf(1).equals(u.isActive))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive"));
  }

  private static boolean sameText(String a, String b) {
    return a != null && b != null && !a.trim().isEmpty() && a.trim().equalsIgnoreCase(b.trim());
  }

  private static boolean isNationalMaker(AppUser u) {
    return "SUPERADMIN_DIT".equals(u.role) || "HO_MARKETING".equals(u.role);
  }

  // Who may change (edit, submit, attach files to) a project: DIT / HO Marketing anywhere,
  // ZO makers only inside their own zone (or their own drafts). Everyone else: no.
  private void assertCanEdit(AppUser user, Project p) {
    if (isNationalMaker(user)) return;
    boolean isCreator = user.username.equalsIgnoreCase(p.createdBy);
    if ("ZO_MAKER".equals(user.role)) {
      if (!isCreator && !sameText(user.zone, p.zoneName))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change projects from other zones");
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot change projects");
  }

  private static boolean isVisibleToPublic(Project p) {
    return "APPROVED".equals(p.status)
      && Integer.valueOf(1).equals(p.isActive)
      && Integer.valueOf(1).equals(p.isPublished);
  }

  // Format checks on everything the maker typed (see ProjectValidator). The pincode is trimmed first.
  private static void validateDetails(Project p) {
    if (p.pinCode != null) p.pinCode = p.pinCode.trim();
    List<String> problems = ProjectValidator.problems(p);
    if (!problems.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < problems.size(); i++) {
        if (i > 0) sb.append("; ");
        sb.append(problems.get(i));
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please correct: " + sb.toString());
    }
  }

  // What a project must have before it can go to the checker (the 9 mandatory BRD fields + pincode).
  private static void requireSubmittable(Project p) {
    List<String> missing = new ArrayList<>();
    if (p.zoneName == null || p.zoneName.trim().isEmpty()) missing.add("Zone Name");
    if (p.branchName == null || p.branchName.trim().isEmpty()) missing.add("Branch Name");
    if (p.stateName == null || p.stateName.trim().isEmpty()) missing.add("State Name");
    if (p.cityName == null || p.cityName.trim().isEmpty()) missing.add("City Name");
    if (p.projectName == null || p.projectName.trim().isEmpty()) missing.add("Project Name");
    if (p.reraNo == null || p.reraNo.trim().isEmpty()) missing.add("RERARegistrationNo");
    if (p.totalInventory == null || p.totalInventory < 1) missing.add("TotalInventory");
    if (p.builderName == null || p.builderName.trim().isEmpty()) missing.add("BuilderName");
    if (p.projectAddress == null || p.projectAddress.trim().isEmpty()) missing.add("ProjectAddress");
    if (p.pinCode == null || p.pinCode.trim().isEmpty()) missing.add("Pincode");
    if (!missing.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < missing.size(); i++) {
        if (i > 0) sb.append(", ");
        sb.append(missing.get(i));
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Please fill in before submitting: " + sb.toString());
    }
  }

  // Random, non-guessable, collision-checked id (the old time-based id repeated every ~100 seconds).
  private String newProjectId() {
    for (int i = 0; i < 10; i++) {
      String id = "UCR-PROJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
      if (!repo.existsById(id)) return id;
    }
    throw new IllegalStateException("Could not allocate a project id");
  }

  public Project publicById(String id) {
    return repo.findById(id).filter(ProjectService::isVisibleToPublic)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published"));
  }

  // For attaching/removing files: project must exist, be editable, and be inside the caller's jurisdiction.
  @Transactional
  public Project requireEditableProject(String id, String username) {
    Project p = findById(id);
    if (!"DRAFT".equals(p.status) && !"NEEDS_CORRECTION".equals(p.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Files can only be changed while the project is DRAFT or NEEDS_CORRECTION (current: " + p.status + ")");
    }
    assertCanEdit(requireActiveUser(username), p);
    return p;
  }

  // Public: only APPROVED + active + published. Enforces security at service layer.
  public List<Project> publicList(String state, String city, String search) {
    List<Project> all = repo.findByStatusAndIsActiveAndIsPublished("APPROVED", 1, 1);
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

  public List<Project> adminList(String username, String scope) {
    AppUser user = requireActiveUser(username);

    boolean isNationalStaff = Arrays.asList("SUPERADMIN_DIT", "HO_ADMIN", "HO_MARKETING", "HO_RETAIL", "CALL_CENTRE")
        .contains(user.role);
    if (isNationalStaff || "all".equalsIgnoreCase(scope)) {
      return repo.findAll();
    }
    if ("ZO_MAKER".equals(user.role) || "ZO_CHECKER".equals(user.role)) {
      return (user.zone == null || user.zone.trim().isEmpty()) ? Collections.emptyList() : repo.findByZoneNameIgnoreCase(user.zone.trim());
    }
    return Collections.emptyList();
  }

  public Project findById(String id) {
    return repo.findById(id).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Project " + id + " not found"));
  }

  public List<AuditLog> getAuditLogs(String id) {
    return audit.findByEntityIdOrderByCreatedAtDesc(id);
  }

  @Transactional
  public Project saveDraft(Project p, String username) {
    AppUser user = requireActiveUser(username);
    p.id = newProjectId();
    p.status = "DRAFT";
    p.createdBy = username;
    p.checkedBy = null;
    p.checkerComment = null;
    p.isActive = 1;
    p.isPublished = 0;
    p.totalImgCount = 0;
    p.verifiedImgCount = 0;

    // Enforce Maker assigned jurisdiction
    if ("ZO_MAKER".equals(user.role) && user.zone != null && !user.zone.trim().isEmpty()) {
      p.zoneName = user.zone.trim();
    }

    validateDetails(p);
    Project saved = repo.save(p);
    log(saved.id, "CREATE_DRAFT", username, null, saved.projectName);
    return saved;
  }

  @Transactional
  public Project updateDraft(String id, Project updateData, String username) {
    Project p = findById(id);
    if (!"DRAFT".equals(p.status) && !"NEEDS_CORRECTION".equals(p.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit project in status: " + p.status);
    }

    AppUser user = requireActiveUser(username);
    assertCanEdit(user, p);

    // Update fields
    if (updateData.projectName != null) p.projectName = updateData.projectName;
    if (updateData.zoneName != null && isNationalMaker(user)) {
      p.zoneName = updateData.zoneName;
    }
    if (updateData.branchName != null && (isNationalMaker(user) || "ZO_MAKER".equals(user.role))) {
      p.branchName = updateData.branchName;
    }
    if (updateData.stateName != null) p.stateName = updateData.stateName;
    if (updateData.cityName != null) p.cityName = updateData.cityName;
    if (updateData.reraNo != null) p.reraNo = updateData.reraNo;
    if (updateData.propertyId != null) p.propertyId = updateData.propertyId;
    if (updateData.typeOfUnit != null) p.typeOfUnit = updateData.typeOfUnit;
    if (updateData.approvalDate != null) p.approvalDate = updateData.approvalDate;
    if (updateData.totalInventory != null) p.totalInventory = updateData.totalInventory;
    if (updateData.unsoldInventory != null) p.unsoldInventory = updateData.unsoldInventory;
    if (updateData.inventoryAsOnDate != null) p.inventoryAsOnDate = updateData.inventoryAsOnDate;
    if (updateData.amenities != null) p.amenities = updateData.amenities;
    if (updateData.startDate != null) p.startDate = updateData.startDate;
    if (updateData.completionDate != null) p.completionDate = updateData.completionDate;
    if (updateData.aptPerFloor != null) p.aptPerFloor = updateData.aptPerFloor;
    if (updateData.noOfTowers != null) p.noOfTowers = updateData.noOfTowers;
    if (updateData.totalProjectArea != null) p.totalProjectArea = updateData.totalProjectArea;
    if (updateData.carpetMin != null) p.carpetMin = updateData.carpetMin;
    if (updateData.carpetMax != null) p.carpetMax = updateData.carpetMax;
    if (updateData.buildupMin != null) p.buildupMin = updateData.buildupMin;
    if (updateData.buildupMax != null) p.buildupMax = updateData.buildupMax;
    if (updateData.distRailway != null) p.distRailway = updateData.distRailway;
    if (updateData.distAirport != null) p.distAirport = updateData.distAirport;
    if (updateData.avgRate != null) p.avgRate = updateData.avgRate;
    if (updateData.budgetMin != null) p.budgetMin = updateData.budgetMin;
    if (updateData.budgetMax != null) p.budgetMax = updateData.budgetMax;
    if (updateData.builderName != null) p.builderName = updateData.builderName;
    if (updateData.projectAddress != null) p.projectAddress = updateData.projectAddress;
    if (updateData.pinCode != null) p.pinCode = updateData.pinCode;
    if (updateData.promoter1 != null) p.promoter1 = updateData.promoter1;
    if (updateData.promoter2 != null) p.promoter2 = updateData.promoter2;
    if (updateData.promoter3 != null) p.promoter3 = updateData.promoter3;
    if (updateData.promoter4 != null) p.promoter4 = updateData.promoter4;
    if (updateData.promoter5 != null) p.promoter5 = updateData.promoter5;
    if (updateData.salesExecName != null) p.salesExecName = updateData.salesExecName;
    if (updateData.salesMobile != null) p.salesMobile = updateData.salesMobile;
    if (updateData.salesEmail != null) p.salesEmail = updateData.salesEmail;
    if (updateData.website != null) p.website = updateData.website;
    if (updateData.builderRating != null) p.builderRating = updateData.builderRating;
    if (updateData.builderBg != null) p.builderBg = updateData.builderBg;
    if (updateData.branchHeadName != null) p.branchHeadName = updateData.branchHeadName;
    if (updateData.branchHeadNumber != null) p.branchHeadNumber = updateData.branchHeadNumber;
    if (updateData.relExecName != null) p.relExecName = updateData.relExecName;
    if (updateData.relExecNumber != null) p.relExecNumber = updateData.relExecNumber;
    if (updateData.validUpto != null) p.validUpto = updateData.validUpto;
    if (updateData.renewalPending != null) p.renewalPending = updateData.renewalPending;
    if (updateData.approvalSought != null) p.approvalSought = updateData.approvalSought;

    validateDetails(p);
    Project saved = repo.save(p);
    log(id, "EDIT_DRAFT", username, p.status, saved.projectName);
    return saved;
  }

  @Transactional
  public Project submit(String id, String username) {
    Project p = findById(id);
    if (!"DRAFT".equals(p.status) && !"NEEDS_CORRECTION".equals(p.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT or NEEDS_CORRECTION can be submitted");
    }
    assertCanEdit(requireActiveUser(username), p);
    validateDetails(p);
    requireSubmittable(p);
    String from = p.status;
    p.status = "PENDING_APPROVAL";
    Project saved = repo.save(p);
    log(id, "SUBMIT_FOR_APPROVAL", username, from, saved.status);
    return saved;
  }

  @Transactional
  public Project decide(String id, String decision, String checkerUsername, String comment) {
    Project p = findById(id);
    if (!"PENDING_APPROVAL".equals(p.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING_APPROVAL can be decided (current: " + p.status + ")");
    }

    AppUser checkerUser = requireActiveUser(checkerUsername);

    if ("HO_ADMIN".equals(checkerUser.role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Head Office has view-only access on project approvals");
    }

    if (checkerUsername.equals(p.createdBy)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maker cannot decide on own project entry");
    }

    if ("ZO_CHECKER".equals(checkerUser.role)) {
      if (checkerUser.zone == null || checkerUser.zone.trim().isEmpty() || p.zoneName == null
          || !checkerUser.zone.trim().equalsIgnoreCase(p.zoneName.trim())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "ZO Checker (" + checkerUser.zone + ") can only review projects from their own zone (" + p.zoneName + ")");
      }
    }

    switch (decision.toLowerCase()) {
      case "approve":
        p.status = "APPROVED";
        p.isPublished = 1;
        break;
      case "reject":
        requireComment(comment);
        p.status = "REJECTED";
        p.isPublished = 0;
        break;
      case "needs-correction":
        requireComment(comment);
        p.status = "NEEDS_CORRECTION";
        p.isPublished = 0;
        break;
      default:
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown decision: " + decision);
    }

    p.checkedBy = checkerUsername;
    p.checkerComment = comment;
    Project saved = repo.save(p);
    log(id, "CHECKER_" + decision.toUpperCase().replace('-', '_'), checkerUsername, "PENDING_APPROVAL", comment);
    return saved;
  }

  @Transactional
  public Project reduceInventory(String id, int units, String reason, String username) {
    AppUser user = requireActiveUser(username);
    if (!"HO_RETAIL".equals(user.role) && !"SUPERADMIN_DIT".equals(user.role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Head Office Retail and Superadmin can reduce unit counts");
    }
    if (units < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Units to reduce must be at least 1");
    }
    if (reason == null || reason.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required");
    }
    Project p = repo.findByIdForUpdate(id).orElseThrow(() ->
      new ResponseStatusException(HttpStatus.NOT_FOUND, "Project " + id + " not found"));
    if (!"APPROVED".equals(p.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Unit count can be reduced only on approved projects (current: " + p.status + ")");
    }
    if (p.unsoldInventory == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No available-unit count is recorded for this project");
    }
    int before = p.unsoldInventory;
    if (units > before) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Cannot reduce by " + units + ": only " + before + " units are available");
    }
    p.unsoldInventory = before - units;
    p.inventoryAsOnDate = java.time.LocalDate.now();
    Project saved = repo.save(p);
    log(id, "INVENTORY_REDUCED", username, "available: " + before,
      "available: " + saved.unsoldInventory + " (reduced by " + units + "; reason: " + reason.trim() + ")");
    return saved;
  }

  private static void requireComment(String comment) {
    if (comment == null || comment.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A remark is required to reject or return a project");
    }
  }
}
