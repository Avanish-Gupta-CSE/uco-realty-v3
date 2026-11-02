package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// LPS stub: every lead queues a PENDING transfer row. The real LPS push job
// (or bank ESB poller) picks these up later - no live LPS call in MVP.
@Entity
@Table(name = "lead_transfer_log")
public class LeadTransferLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "lead_id", nullable = false)
  public Long leadId;

  @Column(length = 20)
  public String target = "LPS";

  @Column(length = 20)
  public String status = "PENDING";

  @Lob
  @Column(name = "payload")
  public String payload;

  @Column(name = "attempted_at")
  public LocalDateTime attemptedAt;

  @PrePersist
  void onCreate() { attemptedAt = LocalDateTime.now(); }

  // --- JavaBean Getters & Setters ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getLeadId() { return leadId; }
  public void setLeadId(Long leadId) { this.leadId = leadId; }
  public String getTarget() { return target; }
  public void setTarget(String target) { this.target = target; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public LocalDateTime getAttemptedAt() { return attemptedAt; }
  public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
}
