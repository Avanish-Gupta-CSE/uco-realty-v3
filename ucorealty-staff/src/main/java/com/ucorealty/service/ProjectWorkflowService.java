package com.ucorealty.service;

import com.ucorealty.entity.AppUser;
import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectData;
import com.ucorealty.entity.ProjectForm;
import com.ucorealty.entity.ProjectImage;
import com.ucorealty.entity.ProjectStaging;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.repository.ProjectRepository;
import com.ucorealty.repository.ProjectStagingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maker-checker with a STAGING table.
 *
 * The master table (projects) holds only approved records. Whatever a maker does (a new project, an edit, a unit
 * reduction, a deletion) is written to a row of project_staging and edited THERE until he sends it for approval.
 * Only when a checker approves is the row applied to the master table:
 *   CREATE -> a new master row, UPDATE -> the existing master row changes, DELETE -> the master row is marked DELETED.
 * The staging row stays as history (APPROVED / REJECTED / WITHDRAWN). Photos follow the same rule: they belong to the
 * staging row (project_images.staging_id) and become live (project_id) only on approval.
 */
@Service
public class ProjectWorkflowService {
  public static final List<String> OPEN = Arrays.asList(ProjectStaging.DRAFT, ProjectStaging.PENDING, ProjectStaging.NEEDS_CORRECTION);
  private static final List<String> NEW_PROJECT_ROWS = Arrays.asList(
      ProjectStaging.DRAFT, ProjectStaging.PENDING, ProjectStaging.NEEDS_CORRECTION, ProjectStaging.REJECTED);
  public static final int MAX_PHOTOS = 10;
  private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowService.class);

  private final ProjectRepository projects;
  private final ProjectStagingRepository staging;
  private final ProjectImageRepository images;
  private final ProjectService projectService;
  private final MasterDataService masterData;
  private final AuditService audit;

  public ProjectWorkflowService(ProjectRepository projects, ProjectStagingRepository staging, ProjectImageRepository images,
                                ProjectService projectService, MasterDataService masterData, AuditService audit) {
    this.projects = projects;
    this.staging = staging;
    this.images = images;
    this.projectService = projectService;
    this.masterData = masterData;
    this.audit = audit;
  }

  // =====================================================================================================
  //  Reading
  // =====================================================================================================

  public ProjectStaging get(Long id) {
    return staging.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
  }

  public ProjectStaging findOpen(String projectId) {
    return staging.findFirstByProjectIdAndStatusIn(projectId, OPEN).orElse(null);
  }

  /** Who may see a staging row: its maker always; others only once it waits for (or was refused by) a checker. */
  public boolean canSee(AppUser u, ProjectStaging s) {
    if (u.username.equals(s.createdBy)) return true;
    if (ProjectStaging.DRAFT.equals(s.status) || ProjectStaging.NEEDS_CORRECTION.equals(s.status)) return false;
    if (!ProjectStaging.PENDING.equals(s.status) && !ProjectStaging.REJECTED.equals(s.status)) return false;
    return ProjectService.isNational(u) || ProjectService.sameText(u.zone, s.zoneName);
  }

  /** The dashboard of one user: live projects (with their open request) and the maker's unapproved new projects. */
  public List<ProjectRow> dashboard(String username) {
    AppUser u = projectService.requireActiveUser(username);
    List<Project> masters = projectService.approvedList(username);
    Map<String, ProjectStaging> openByProject = new HashMap<>();
    if (!masters.isEmpty()) {
      for (ProjectStaging s : staging.findByProjectIdInAndStatusIn(masters.stream().map(p -> p.id).collect(Collectors.toList()), OPEN)) {
        if (!ProjectStaging.CREATE.equals(s.requestType) && canSee(u, s)) openByProject.put(s.projectId, s);
      }
    }
    List<ProjectRow> rows = new ArrayList<>();
    for (Project p : masters) rows.add(ProjectRow.ofMaster(p, openByProject.get(p.id)));

    boolean zonal = "ZO_MAKER".equals(u.role) || "ZO_CHECKER".equals(u.role);
    for (ProjectStaging s : staging.findByRequestTypeAndStatusIn(ProjectStaging.CREATE, NEW_PROJECT_ROWS)) {
      if (!canSee(u, s)) continue;
      if (zonal && !u.username.equals(s.createdBy) && !ProjectService.sameText(u.zone, s.zoneName)) continue;
      rows.add(ProjectRow.ofStaging(s));
    }
    return rows;
  }

  /** A customer-style preview of an unapproved request (a transient copy, never saved). */
  public Project previewOf(ProjectStaging s) {
    Project p = new Project();
    ProjectData.copyBusinessFields(s, p, false);
    p.id = s.projectId;
    p.status = s.status;
    p.isPublished = 0;
    return p;
  }

  public static class FieldChange {
    public final String label, oldValue, newValue;
    FieldChange(String label, String oldValue, String newValue) { this.label = label; this.oldValue = oldValue; this.newValue = newValue; }
    public String getLabel() { return label; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
  }

  /** UPDATE: the fields that differ between the live project and the maker's working copy. */
  public List<FieldChange> diff(Project live, ProjectStaging s) {
    List<FieldChange> out = new ArrayList<>();
    for (Field f : ProjectData.businessFields()) {
      if ("zoneName".equals(f.getName())) continue;
      String a = show(ProjectData.value(live, f)), b = show(ProjectData.value(s, f));
      if (!a.equals(b)) out.add(new FieldChange(label(f.getName()), a, b));
    }
    return out;
  }

  /** CREATE: every filled field of the new project, for the checker's review page. */
  public List<FieldChange> contents(ProjectStaging s) {
    List<FieldChange> out = new ArrayList<>();
    for (Field f : ProjectData.businessFields()) {
      String v = show(ProjectData.value(s, f));
      if (!"-".equals(v)) out.add(new FieldChange(label(f.getName()), "", v));
    }
    return out;
  }

  public Set<Long> removedImageIds(ProjectStaging s) {
    Set<Long> ids = new LinkedHashSet<>();
    if (s.removeImageIds != null) {
      for (String x : s.removeImageIds.split(",")) if (!x.trim().isEmpty()) ids.add(Long.valueOf(x.trim()));
    }
    return ids;
  }

  public List<ProjectImage> removedImages(ProjectStaging s) { return images.findAllById(removedImageIds(s)); }

  public List<ProjectImage> newImages(ProjectStaging s) { return images.findByStagingId(s.id); }

  /** The photos the maker sees on the form: live ones not marked for removal (UPDATE), plus those added in this request. */
  public List<ProjectImage> photosForForm(ProjectStaging s) {
    List<ProjectImage> out = new ArrayList<>();
    if (ProjectStaging.UPDATE.equals(s.requestType)) {
      Set<Long> removed = removedImageIds(s);
      for (ProjectImage i : images.findByProjectId(s.projectId)) if (!removed.contains(i.id)) out.add(i);
    }
    out.addAll(images.findByStagingId(s.id));
    return out;
  }

  /** The maker; or, while the request waits for approval, staff of the right zone who are not makers. */
  public void requireCanView(ProjectStaging s, String username) {
    if (username.equals(s.createdBy)) return;
    AppUser u = projectService.requireActiveUser(username);
    if (!ProjectStaging.PENDING.equals(s.status))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This request is not waiting for approval");
    if ("ZO_MAKER".equals(u.role))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the maker who raised this request can open it");
    ProjectService.requireZoneAccess(u, zoneOf(s));
  }

  // =====================================================================================================
  //  Maker
  // =====================================================================================================

  /** First save of a NEW project: creates the working copy (staging row) with its project code. */
  @Transactional
  public ProjectStaging createDraft(ProjectForm form, Set<String> sent, String username) {
    AppUser user = projectService.requireActiveUser(username);
    ProjectService.requireMakerRole(user);
    ProjectStaging s = new ProjectStaging();
    s.requestType = ProjectStaging.CREATE;
    s.status = ProjectStaging.DRAFT;
    s.createdBy = username;
    s.projectId = newProjectCode();
    applySent(form, s, sent, false);
    pinZone(user, s);
    ProjectService.validateDetails(s);
    s = staging.save(s);
    audit.logActivity(username, "CREATE_DRAFT", "PROJECT", s.projectId, null, s.projectName);
    log.info("[UCO WORKFLOW] {} saved a first draft of new project {} (request {})", username, s.projectId, s.id);
    return s;
  }

  /** Opens the maker's working copy of a LIVE project (created from the approved values). */
  @Transactional
  public ProjectStaging openUpdateDraft(String projectId, String username) {
    AppUser user = projectService.requireActiveUser(username);
    ProjectService.requireMakerRole(user);
    Project p = liveProject(projectId);
    ProjectService.requireZoneAccess(user, p.zoneName);

    ProjectStaging open = findOpen(projectId);
    if (open != null) {
      if (!username.equals(open.createdBy))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this project is already in progress (by " + open.createdBy + ")");
      if (!ProjectStaging.UPDATE.equals(open.requestType))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "A deletion request for this project is already waiting for approval");
      if (ProjectStaging.PENDING.equals(open.status))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Your change is waiting for the checker. Withdraw it first if you want to edit it again");
      return open;
    }
    ProjectStaging s = fromLive(p, ProjectStaging.UPDATE, ProjectStaging.DRAFT, username);
    s = saveNew(s);
    audit.logActivity(username, "CHANGE_DRAFT_STARTED", "PROJECT", projectId, "Started editing the approved project (a private working copy; the live project is unchanged)");
    log.info("[UCO WORKFLOW] {} started editing approved project {} (request {})", username, projectId, s.id);
    return s;
  }

  /** Saves the maker's edits into HIS working copy. Only the fields the form sent are updated; the master is not touched. */
  @Transactional
  public ProjectStaging saveDraft(Long id, ProjectForm form, Set<String> sent, String username) {
    ProjectStaging s = ownEditable(id, username);
    boolean update = ProjectStaging.UPDATE.equals(s.requestType);
    AppUser user = projectService.requireActiveUser(username);
    applySent(form, s, sent, update);
    if (!update) pinZone(user, s);
    ProjectService.validateDetails(s);
    s = staging.save(s);
    audit.logActivity(username, update ? "CHANGE_DRAFT_SAVED" : "EDIT_DRAFT", "PROJECT", s.projectId, null,
        update ? "Saved a draft change of the approved project (not sent for approval)" : s.projectName);
    log.info("[UCO WORKFLOW] {} saved draft {} of project {} ({})", username, s.id, s.projectId, s.requestType);
    return s;
  }

  /** Removes a photo from the request: one added in this request is deleted, a live one is marked and goes only on approval. */
  @Transactional
  public void removePhoto(Long id, Long imageId, String username) {
    ProjectStaging s = ownEditable(id, username);
    ProjectImage img = images.findById(imageId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
    if (id.equals(img.stagingId)) {
      images.delete(img);
    } else if (ProjectStaging.UPDATE.equals(s.requestType) && s.projectId.equals(img.projectId)) {
      Set<Long> ids = removedImageIds(s);
      ids.add(imageId);
      s.removeImageIds = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
      staging.save(s);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That photo does not belong to this request");
    }
    log.info("[UCO WORKFLOW] {} removed photo {} from request {} of project {}", username, imageId, id, s.projectId);
  }

  @Transactional
  public ProjectStaging submit(Long id, String username) {
    ProjectStaging s = ownEditable(id, username);
    ProjectService.validateDetails(s);
    ProjectService.requireSubmittable(s);
    boolean update = ProjectStaging.UPDATE.equals(s.requestType);
    if (update) {
      Project live = liveProject(s.projectId);
      List<FieldChange> diff = diff(live, s);
      if (diff.isEmpty() && removedImageIds(s).isEmpty() && images.findByStagingId(s.id).isEmpty())
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing was changed: edit a value or a photo first");
      s.summary = trim(summaryOf(diff, s), 1000);
    } else {
      s.summary = "New project";
    }
    s.status = ProjectStaging.PENDING;
    s.submittedAt = LocalDateTime.now();
    s.checkerComment = null;
    s = staging.save(s);
    audit.logActivity(username, update ? "CHANGE_SUBMITTED" : "SUBMIT_FOR_APPROVAL", "PROJECT", s.projectId, null, trim(s.summary, 1000));
    log.info("[UCO WORKFLOW] {} sent request {} ({}) of project {} for approval", username, s.id, s.requestType, s.projectId);
    return s;
  }

  /** Unit reduction is an edit of a live project: a one-step request that waits for a checker. */
  @Transactional
  public ProjectStaging requestUnitReduction(String projectId, int units, String reason, String username) {
    AppUser user = projectService.requireActiveUser(username);
    ProjectService.requireMakerRole(user);
    Project p = liveProject(projectId);
    ProjectService.requireZoneAccess(user, p.zoneName);
    requireNoOpen(projectId);
    if (units < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Units to reduce must be at least 1");
    if (reason == null || reason.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required");
    ProjectService.requireNoMarkup("The reason", reason);
    if (p.unsoldInventory == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No available-unit count is recorded for this project");
    if (units > p.unsoldInventory)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reduce by " + units + ": only " + p.unsoldInventory + " units are available");

    ProjectStaging s = fromLive(p, ProjectStaging.UPDATE, ProjectStaging.PENDING, username);
    s.unsoldInventory = p.unsoldInventory - units;
    s.inventoryAsOnDate = LocalDate.now();
    s.reason = trim(reason.trim(), 1000);
    s.summary = trim("Reduce available units " + p.unsoldInventory + " -> " + s.unsoldInventory + " (reason: " + reason.trim() + ")", 1000);
    s.submittedAt = LocalDateTime.now();
    s = saveNew(s);
    audit.logActivity(username, "CHANGE_SUBMITTED", "PROJECT", projectId, null, s.summary);
    log.info("[UCO WORKFLOW] {} requested a unit reduction of {} on project {} (request {})", username, units, projectId, s.id);
    return s;
  }

  @Transactional
  public ProjectStaging requestDelete(String projectId, String reason, String username) {
    AppUser user = projectService.requireActiveUser(username);
    ProjectService.requireMakerRole(user);
    Project p = liveProject(projectId);
    ProjectService.requireZoneAccess(user, p.zoneName);
    requireNoOpen(projectId);
    if (reason == null || reason.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason for the deletion is required");
    ProjectService.requireNoMarkup("The reason", reason);

    ProjectStaging s = fromLive(p, ProjectStaging.DELETE, ProjectStaging.PENDING, username);
    s.reason = trim(reason.trim(), 1000);
    s.summary = trim("Delete project (reason: " + reason.trim() + ")", 1000);
    s.submittedAt = LocalDateTime.now();
    s = saveNew(s);
    audit.logActivity(username, "DELETE_REQUESTED", "PROJECT", projectId, null, s.summary);
    log.info("[UCO WORKFLOW] {} requested deletion of project {} (request {})", username, projectId, s.id);
    return s;
  }

  /** The maker takes his request back (draft, returned or waiting). A live project is unaffected. */
  @Transactional
  public void withdraw(Long id, String username) {
    ProjectStaging s = get(id);
    if (!username.equals(s.createdBy)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the maker who raised this request can withdraw it");
    if (!s.isOpen()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This request is already closed (" + s.status + ")");
    images.deleteAll(images.findByStagingId(s.id));
    s.status = ProjectStaging.WITHDRAWN;
    s.decidedAt = LocalDateTime.now();
    staging.save(s);
    audit.logActivity(username, ProjectStaging.CREATE.equals(s.requestType) ? "DRAFT_DISCARDED" : "CHANGE_WITHDRAWN", "PROJECT", s.projectId, null,
        "Withdrawn by the maker" + (ProjectStaging.CREATE.equals(s.requestType) ? "" : "; the live project is unchanged"));
    log.info("[UCO WORKFLOW] {} withdrew request {} ({}) of project {}", username, s.id, s.requestType, s.projectId);
  }

  // =====================================================================================================
  //  Checker
  // =====================================================================================================

  @Transactional
  public ProjectStaging decide(Long id, String decision, String comment, String checkerUsername) {
    ProjectStaging s = get(id);
    if (!ProjectStaging.PENDING.equals(s.status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a request waiting for approval can be decided (current: " + s.status + ")");
    AppUser checker = projectService.requireActiveUser(checkerUsername);
    ProjectService.requireCheckerRole(checker);
    if (checkerUsername.equals(s.createdBy)) {
      log.warn("[UCO WORKFLOW] REFUSED: {} tried to decide on their own request {} of project {}", checkerUsername, s.id, s.projectId);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A maker cannot decide on his own request");
    }
    if ("ZO_CHECKER".equals(checker.role) && !ProjectService.sameText(checker.zone, zoneOf(s))) {
      log.warn("[UCO WORKFLOW] REFUSED: checker {} (zone {}) tried to decide on request {} of zone {}", checkerUsername, checker.zone, s.id, zoneOf(s));
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "ZO Checker (" + checker.zone + ") can only review projects from their own zone (" + zoneOf(s) + ")");
    }

    ProjectService.requireNoMarkup("The remark", comment);
    String d = decision == null ? "" : decision.toLowerCase();
    String type = s.requestType;
    try {
      switch (d) {
        case "approve":
          if (ProjectStaging.CREATE.equals(type)) applyCreate(s, checkerUsername, comment);
          else if (ProjectStaging.UPDATE.equals(type)) applyUpdate(s, checkerUsername, comment);
          else applyDelete(s, checkerUsername, comment);
          s.status = ProjectStaging.APPROVED;
          audit.logActivity(checkerUsername, ProjectStaging.CREATE.equals(type) ? "CHECKER_APPROVE" : ProjectStaging.UPDATE.equals(type) ? "CHANGE_APPROVED" : "DELETE_APPROVED",
              "PROJECT", s.projectId, null, trim(s.summary, 1000));
          break;
        case "reject":
          ProjectService.requireComment(comment);
          s.status = ProjectStaging.REJECTED;
          if (!ProjectStaging.CREATE.equals(type)) images.deleteAll(images.findByStagingId(s.id));   // they will never go live
          audit.logActivity(checkerUsername, ProjectStaging.CREATE.equals(type) ? "CHECKER_REJECT" : ProjectStaging.UPDATE.equals(type) ? "CHANGE_REJECTED" : "DELETE_REJECTED",
              "PROJECT", s.projectId, null, trim(comment, 1000));
          break;
        case "needs-correction":
          if (ProjectStaging.DELETE.equals(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A deletion request can only be approved or rejected");
          ProjectService.requireComment(comment);
          s.status = ProjectStaging.NEEDS_CORRECTION;
          audit.logActivity(checkerUsername, ProjectStaging.CREATE.equals(type) ? "CHECKER_NEEDS_CORRECTION" : "CHANGE_RETURNED", "PROJECT", s.projectId, null, trim(comment, 1000));
          break;
        default:
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown decision: " + decision);
      }
      s.checkedBy = checkerUsername;
      s.checkerComment = trim(comment, 1000);
      s.decidedAt = LocalDateTime.now();
      s = staging.saveAndFlush(s);
      log.info("[UCO WORKFLOW] {} decided '{}' on {} request {} of project {} (maker {}) -> {}", checkerUsername, d, type, s.id, s.projectId, s.createdBy, s.status);
      return s;
    } catch (ObjectOptimisticLockingFailureException e) {
      log.warn("[UCO WORKFLOW] Decision on request {} by {} lost a race: someone else changed it at the same moment", id, checkerUsername);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Someone else changed this at the same moment. Reload and check it again");
    }
  }

  // ---- applying an approved request to the master table ----

  private void applyCreate(ProjectStaging s, String checker, String comment) {
    if (projects.existsById(s.projectId))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A project with this code already exists");
    ProjectService.validateDetails(s);
    ProjectService.requireSubmittable(s);
    Project p = new Project();
    ProjectData.copyBusinessFields(s, p, false);
    p.id = s.projectId;
    p.status = Project.APPROVED;
    p.isActive = 1;
    p.isPublished = 1;
    p.createdBy = s.createdBy;
    p.checkedBy = checker;
    p.checkerComment = trim(comment, 1000);
    projects.saveAndFlush(p);
    goLive(s, p);
  }

  private void applyUpdate(ProjectStaging s, String checker, String comment) {
    Project p = liveProject(s.projectId);
    if (s.baseVersion != null && !s.baseVersion.equals(p.rowVersion)) {
      log.warn("[UCO WORKFLOW] Cannot apply {} request {}: live project {} changed after the request was opened", s.requestType, s.id, s.projectId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "The live project changed after this request was opened. Return it to the maker");
    }
    ProjectService.validateDetails(s);
    ProjectData.copyBusinessFields(s, p, true);          // the zone never changes
    for (Long imageId : removedImageIds(s)) {
      images.findById(imageId).filter(i -> p.id.equals(i.projectId)).ifPresent(images::delete);
    }
    p.checkedBy = checker;
    p.checkerComment = trim(comment, 1000);
    projects.saveAndFlush(p);
    goLive(s, p);
  }

  private void applyDelete(ProjectStaging s, String checker, String comment) {
    Project p = liveProject(s.projectId);
    if (s.baseVersion != null && !s.baseVersion.equals(p.rowVersion)) {
      log.warn("[UCO WORKFLOW] Cannot apply {} request {}: live project {} changed after the request was opened", s.requestType, s.id, s.projectId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "The live project changed after this request was opened. Return it to the maker");
    }
    p.status = Project.DELETED;
    p.isPublished = 0;
    p.isActive = 0;
    p.deletedAt = LocalDateTime.now();
    p.deletedBy = s.createdBy;
    p.checkedBy = checker;
    p.checkerComment = trim(comment, 1000);
    projects.saveAndFlush(p);
  }

  /** The photos of the approved request become the project's live photos. */
  private void goLive(ProjectStaging s, Project p) {
    for (ProjectImage img : images.findByStagingId(s.id)) {
      img.projectId = p.id;
      img.stagingId = null;
      img.isVerified = 1;
      images.save(img);
    }
    images.flush();
    p.totalImgCount = images.findByProjectId(p.id).size();
    projects.save(p);
  }

  // =====================================================================================================
  //  Helpers
  // =====================================================================================================

  public int photoCount(ProjectStaging s) { return photosForForm(s).size(); }

  private ProjectStaging ownEditable(Long id, String username) {
    ProjectStaging s = get(id);
    if (!username.equals(s.createdBy))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the maker who raised this request can edit it");
    if (ProjectStaging.DELETE.equals(s.requestType))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A deletion request has nothing to edit");
    if (!ProjectStaging.DRAFT.equals(s.status) && !ProjectStaging.NEEDS_CORRECTION.equals(s.status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "It can be edited only while it is a draft or returned for correction (current: " + s.status + ")");
    AppUser user = projectService.requireActiveUser(username);
    ProjectService.requireMakerRole(user);
    if (ProjectStaging.UPDATE.equals(s.requestType)) {
      liveProject(s.projectId);
    }
    ProjectService.requireZoneAccess(user, zoneOf(s));
    return s;
  }

  private Project liveProject(String projectId) {
    Project p = projects.findById(projectId).filter(x -> !Project.DELETED.equals(x.status))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project " + projectId + " not found"));
    if (!Project.APPROVED.equals(p.status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved projects can be changed this way");
    return p;
  }

  private String zoneOf(ProjectStaging s) {
    if (!ProjectStaging.CREATE.equals(s.requestType)) {
      return projects.findById(s.projectId).map(p -> p.zoneName).orElse(s.zoneName);
    }
    return s.zoneName;
  }

  private void requireNoOpen(String projectId) {
    ProjectStaging open = findOpen(projectId);
    if (open != null)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this project is already in progress (" + open.requestType + ", " + open.status + ", by " + open.createdBy + ")");
  }

  private ProjectStaging fromLive(Project p, String type, String status, String username) {
    ProjectStaging s = new ProjectStaging();
    ProjectData.copyBusinessFields(p, s, false);
    s.projectId = p.id;
    s.requestType = type;
    s.status = status;
    s.baseVersion = p.rowVersion;
    s.createdBy = username;
    return s;
  }

  private ProjectStaging saveNew(ProjectStaging s) {
    try {
      return staging.saveAndFlush(s);
    } catch (DataIntegrityViolationException e) {     // the database allows only one open request per project
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for this project is already in progress");
    }
  }

  /** Copies the fields the form sent onto the working copy (the zone of an UPDATE never changes). */
  private void applySent(ProjectForm form, ProjectStaging s, Set<String> sent, boolean keepZone) {
    for (Field f : ProjectData.businessFields()) {
      if (keepZone && "zoneName".equals(f.getName())) continue;
      if (sent.contains(f.getName())) ProjectData.set(s, f, ProjectData.value(form, f));
    }
  }

  /** A zonal maker's zone comes from his login; a Head Office maker must pick one of the 49 official zones. */
  private void pinZone(AppUser user, ProjectStaging s) {
    if ("ZO_MAKER".equals(user.role) && user.zone != null && !user.zone.trim().isEmpty()) {
      s.zoneName = user.zone.trim();
    } else if (s.zoneName != null && !s.zoneName.trim().isEmpty()) {
      String z = s.zoneName.trim();
      String official = masterData.getZones().stream().filter(x -> x.equalsIgnoreCase(z)).findFirst().orElse(null);
      if (official == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please correct: Zone must be one of the official Zonal Offices");
      s.zoneName = official;
    }
  }

  private String newProjectCode() {
    for (int i = 0; i < 10; i++) {
      String code = "UCR-PROJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
      if (!projects.existsById(code) && staging.findByProjectIdOrderByIdDesc(code).isEmpty()) return code;
    }
    throw new IllegalStateException("Could not allocate a project code");
  }

  private String summaryOf(List<FieldChange> diff, ProjectStaging s) {
    List<String> parts = new ArrayList<>();
    for (FieldChange c : diff) parts.add(c.label + ": " + c.oldValue + " -> " + c.newValue);
    if (!removedImageIds(s).isEmpty()) parts.add(removedImageIds(s).size() + " photo(s) removed");
    int added = images.findByStagingId(s.id).size();
    if (added > 0) parts.add(added + " photo(s) added");
    return String.join("; ", parts);
  }

  private static String show(Object v) { return v == null || v.toString().trim().isEmpty() ? "-" : v.toString(); }

  private static String label(String field) {
    String s = field.replaceAll("([A-Z])", " $1").toLowerCase(Locale.ROOT).trim();
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private static String trim(String s, int max) {
    if (s == null) return null;
    return s.length() <= max ? s : s.substring(0, max - 3) + "...";
  }
}
