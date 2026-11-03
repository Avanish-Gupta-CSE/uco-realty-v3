package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 50)
  public String username;

  @Column(nullable = false, length = 30)
  public String role; // SUPERADMIN_DIT, HO_MARKETING, HO_RETAIL, ZO_MAKER, ZO_CHECKER, CALL_CENTRE

  public String zone;
  public String branch;

  @Column(name = "is_active")
  public Integer isActive = 1;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @PrePersist
  void onCreate() { createdAt = LocalDateTime.now(); }

  // --- JavaBean Getters & Setters for JSP EL support ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getZone() { return zone; }
  public void setZone(String zone) { this.zone = zone; }
  public String getBranch() { return branch; }
  public void setBranch(String branch) { this.branch = branch; }
  public Integer getIsActive() { return isActive; }
  public void setIsActive(Integer isActive) { this.isActive = isActive; }
  public Integer getActive() { return isActive; }
  public void setActive(Integer active) { this.isActive = active; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  private static final java.time.format.DateTimeFormatter DATE_FORMATTER =
      java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

  public String getFormattedDate() {
    if (createdAt == null) {
      return "-";
    }
    return createdAt.format(DATE_FORMATTER);
  }

  public String getFormattedCreatedAt() {
    return getFormattedDate();
  }
}
