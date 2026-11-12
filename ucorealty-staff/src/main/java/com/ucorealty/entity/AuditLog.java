package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Every workflow change lands here: who did what to which record, when.
@Entity
@Table(name = "realty_audit_log")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "entity_type", nullable = false, length = 50)
  public String entityType;

  @Column(name = "entity_id", nullable = false, length = 100)
  public String entityId;

  @Column(nullable = false, length = 50)
  public String action;

  @Column(name = "by_user", length = 50)
  public String byUser;

  @Column(name = "old_val", length = 1000)
  public String oldVal;

  @Column(name = "new_val", length = 1000)
  public String newVal;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public AuditLog() {}

  public AuditLog(String entityType, String entityId, String action, String byUser, String oldVal, String newVal) {
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
    this.byUser = byUser;
    this.oldVal = oldVal;
    this.newVal = newVal;
    this.createdAt = LocalDateTime.now();
  }

  // --- View Helpers for JSP ---
  public String getFormattedCreatedAt() {
    if (createdAt == null) return "-";
    return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
  }

  public String getDetails() {
    if (newVal != null && !newVal.trim().isEmpty()) {
      if (oldVal != null && !oldVal.trim().isEmpty()) {
        return oldVal + " \u2192 " + newVal;
      }
      return newVal;
    }
    return (oldVal != null && !oldVal.trim().isEmpty()) ? oldVal : "-";
  }

  // --- Getters (the pages read the values; no setters needed) ---
  public Long getId() { return id; }
  public String getEntityType() { return entityType; }
  public String getEntityId() { return entityId; }
  public String getAction() { return action; }
  public String getByUser() { return byUser; }
  public String getOldVal() { return oldVal; }
  public String getNewVal() { return newVal; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}
