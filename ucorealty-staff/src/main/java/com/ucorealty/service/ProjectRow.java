package com.ucorealty.service;

import com.ucorealty.entity.Project;
import com.ucorealty.entity.ProjectStaging;

import java.time.LocalDateTime;

/**
 * One line of the dashboard. A line is either a LIVE project (master row, status APPROVED) or a maker's NEW project
 * that is not approved yet (a CREATE row of the staging table: DRAFT, PENDING_APPROVAL, NEEDS_CORRECTION, REJECTED).
 * A live project can carry one open request (UPDATE or DELETE) that is shown next to it.
 */
public class ProjectRow {
  public String id;
  public String projectName, zoneName, cityName, stateName, builderName, reraNo, branchName, createdBy, checkerComment;
  public Integer unsoldInventory, totalInventory;
  public LocalDateTime updatedAt;
  public String status;            // APPROVED | DRAFT | PENDING_APPROVAL | NEEDS_CORRECTION | REJECTED
  public boolean master;           // true: live record in the projects table

  // the request attached to this line: the CREATE row itself, or the open UPDATE / DELETE of a live project
  public Long stagingId;
  public String requestType;       // CREATE | UPDATE | DELETE | null
  public String stagingStatus;     // DRAFT | PENDING_APPROVAL | NEEDS_CORRECTION | REJECTED | null
  public String stagingOwner;      // the maker
  public String stagingComment;    // the checker's remark on that request

  static ProjectRow ofMaster(Project p, ProjectStaging s) {
    ProjectRow r = new ProjectRow();
    r.id = p.id;
    r.projectName = p.projectName; r.zoneName = p.zoneName; r.cityName = p.cityName; r.stateName = p.stateName;
    r.builderName = p.builderName; r.reraNo = p.reraNo; r.branchName = p.branchName; r.unsoldInventory = p.unsoldInventory; r.totalInventory = p.totalInventory;
    r.createdBy = p.createdBy; r.checkerComment = p.checkerComment; r.updatedAt = p.updatedAt;
    r.status = Project.APPROVED;
    r.master = true;
    if (s != null) r.attach(s);
    return r;
  }

  static ProjectRow ofStaging(ProjectStaging s) {
    ProjectRow r = new ProjectRow();
    r.id = s.projectId;
    r.projectName = s.projectName; r.zoneName = s.zoneName; r.cityName = s.cityName; r.stateName = s.stateName;
    r.builderName = s.builderName; r.reraNo = s.reraNo; r.branchName = s.branchName; r.unsoldInventory = s.unsoldInventory; r.totalInventory = s.totalInventory;
    r.createdBy = s.createdBy; r.checkerComment = s.checkerComment; r.updatedAt = s.updatedAt;
    r.status = s.status;
    r.master = false;
    r.attach(s);
    return r;
  }

  private void attach(ProjectStaging s) {
    stagingId = s.id; requestType = s.requestType; stagingStatus = s.status; stagingOwner = s.createdBy; stagingComment = s.checkerComment;
  }

  /** A request of this line is waiting for a checker. */
  public boolean isWaitingForChecker() { return ProjectStaging.PENDING.equals(stagingStatus); }

  /** Live project with an UPDATE / DELETE waiting for a checker: "CHANGE_PENDING" | "DELETE_PENDING" | null. */
  public String getPendingKind() {
    if (!master || !isWaitingForChecker()) return null;
    return ProjectStaging.DELETE.equals(requestType) ? "DELETE_PENDING" : "CHANGE_PENDING";
  }

  public String getId() { return id; }
  public String getProjectName() { return projectName; }
  public String getZoneName() { return zoneName; }
  public String getCityName() { return cityName; }
  public String getStateName() { return stateName; }
  public String getBuilderName() { return builderName; }
  public String getCreatedBy() { return createdBy; }
  public String getCheckerComment() { return checkerComment; }
  public Integer getUnsoldInventory() { return unsoldInventory; }
  public Integer getTotalInventory() { return totalInventory; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public String getStatus() { return status; }
  public boolean isMaster() { return master; }
  public Long getStagingId() { return stagingId; }
  public String getRequestType() { return requestType; }
  public String getStagingStatus() { return stagingStatus; }
  public String getStagingOwner() { return stagingOwner; }
  public String getStagingComment() { return stagingComment; }
}
