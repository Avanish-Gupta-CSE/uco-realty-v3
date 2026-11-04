package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * The MAKER'S WORKING COPY (staging table). Everything a maker does happens in this row until he sends it for
 * approval; the master table is not touched. Only when a checker approves is it applied to {@link Project}.
 *
 *   CREATE  a new project            (on approval: inserted into projects)
 *   UPDATE  edit / unit reduction    (on approval: copied onto the existing project)
 *   DELETE  deletion request         (on approval: the project is marked DELETED)
 *
 * Approved, rejected and withdrawn rows stay as the history of who asked for what.
 */
@Entity
@Table(name = "project_staging")
public class ProjectStaging extends ProjectData {
  public static final String CREATE = "CREATE";
  public static final String UPDATE = "UPDATE";
  public static final String DELETE = "DELETE";

  public static final String DRAFT = "DRAFT";
  public static final String PENDING = "PENDING_APPROVAL";
  public static final String NEEDS_CORRECTION = "NEEDS_CORRECTION";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";
  public static final String WITHDRAWN = "WITHDRAWN";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // the project code: allocated when the CREATE draft is first saved, and becomes projects.id on approval
  @Column(name = "project_id", nullable = false, length = 20)
  public String projectId;

  @Column(name = "request_type", nullable = false, length = 10)
  public String requestType;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(name = "remove_image_ids", length = 500)
  public String removeImageIds;

  @Column(length = 1000)
  public String reason;

  @Column(length = 1000)
  public String summary;

  @Column(name = "base_version")
  public Long baseVersion;

  @Version
  @Column(name = "row_version")
  public Long rowVersion;

  @Column(name = "created_by", nullable = false, length = 50)
  public String createdBy;

  @Column(name = "checked_by", length = 50)
  public String checkedBy;

  @Column(name = "checker_comment", length = 1000)
  public String checkerComment;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @Column(name = "updated_at")
  public LocalDateTime updatedAt;

  @Column(name = "submitted_at")
  public LocalDateTime submittedAt;

  @Column(name = "decided_at")
  public LocalDateTime decidedAt;

  @PrePersist
  void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

  @PreUpdate
  void onUpdate() { updatedAt = LocalDateTime.now(); }

  /** DRAFT, PENDING_APPROVAL or NEEDS_CORRECTION: still being worked on or waiting for a checker. */
  public boolean isOpen() { return DRAFT.equals(status) || PENDING.equals(status) || NEEDS_CORRECTION.equals(status); }

  public Long getId() { return id; }
  public String getProjectId() { return projectId; }
  public String getRequestType() { return requestType; }
  public String getStatus() { return status; }
  public String getReason() { return reason; }
  public String getSummary() { return summary; }
  public String getCreatedBy() { return createdBy; }
  public String getCheckedBy() { return checkedBy; }
  public String getCheckerComment() { return checkerComment; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
}
