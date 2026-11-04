package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * MASTER record: the project as approved and live (customers see exactly this). A row here is created only when a
 * checker approves a CREATE request, and is changed only when a checker approves an UPDATE or DELETE request
 * (see ProjectStaging and ProjectWorkflowService). Status is APPROVED, or DELETED (kept for the logs, shown nowhere).
 */
@Entity
@Table(name = "projects")
public class Project extends ProjectData {
  public static final String APPROVED = "APPROVED";
  public static final String DELETED = "DELETED";

  @Id
  @Column(name = "id", length = 20)
  public String id;

  @Column(name = "sr_no")
  public Long srNo;

  @Column(name = "certificate_path", length = 500)
  public String certificatePath;

  @Column(name = "floor_plan_path", length = 500)
  public String floorPlanPath;

  @Column(name = "total_img_count")
  public Integer totalImgCount = 0;

  @Column(name = "verified_img_count")
  public Integer verifiedImgCount = 0;

  @Column(name = "status", nullable = false)
  public String status = APPROVED;

  @Column(name = "is_active")
  public Integer isActive = 1;

  @Column(name = "is_published")
  public Integer isPublished = 1;

  @Column(name = "created_by")
  public String createdBy;          // the maker who raised the project

  @Column(name = "checked_by")
  public String checkedBy;          // the checker who last approved it

  @Column(name = "checker_comment", length = 1000)
  public String checkerComment;

  @Column(name = "deleted_at")
  public LocalDateTime deletedAt;

  @Column(name = "deleted_by", length = 50)
  public String deletedBy;

  // optimistic locking: also lets an UPDATE/DELETE request notice that the master row moved on since it was opened
  @Version
  @Column(name = "row_version")
  public Long rowVersion;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @Column(name = "updated_at")
  public LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() { updatedAt = LocalDateTime.now(); }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Long getSrNo() { return srNo; }
  public String getStatus() { return status; }
  public Integer getIsActive() { return isActive; }
  public Integer getActive() { return isActive; }
  public Integer getIsPublished() { return isPublished; }
  public Integer getPublished() { return isPublished; }
  public String getCreatedBy() { return createdBy; }
  public String getCheckedBy() { return checkedBy; }
  public String getCheckerComment() { return checkerComment; }
  public LocalDateTime getDeletedAt() { return deletedAt; }
  public String getDeletedBy() { return deletedBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
