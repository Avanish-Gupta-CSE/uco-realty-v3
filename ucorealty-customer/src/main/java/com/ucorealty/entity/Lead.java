package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
public class Lead {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "customer_name", nullable = false)
  public String customerName;

  @Column(nullable = false, length = 15)
  public String phone;

  @Column(name = "project_id", length = 20)
  public String projectId;

  @Column(name = "branch_selected")
  public String branchSelected;

  @Column(length = 30)
  public String status;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    if (status == null || status.trim().isEmpty()) {
      status = (projectId != null && !projectId.trim().isEmpty()) ? "Showed interest in Home Loan" : "Visitor";
    } else if ("VISITOR".equalsIgnoreCase(status)) {
      status = "Visitor";
    } else if ("SUCCESSFUL_APPLIED".equalsIgnoreCase(status)) {
      status = "Showed interest in Home Loan";
    }
  }

  // --- JavaBean Getters & Setters for JSP EL support ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCustomerName() { return customerName; }
  public void setCustomerName(String customerName) { this.customerName = customerName; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getProjectId() { return projectId; }
  public void setProjectId(String projectId) { this.projectId = projectId; }
  public String getBranchSelected() { return branchSelected; }
  public void setBranchSelected(String branchSelected) { this.branchSelected = branchSelected; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
