package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "realty_login_audit")
public class LoginAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", length = 100)
  private String username;

  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Column(name = "success", nullable = false)
  private boolean success;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "login_time")
  private LocalDateTime loginTime;

  @PrePersist
  void onCreate() {
    if (loginTime == null) {
      loginTime = LocalDateTime.now();
    }
  }

  public LoginAudit() {}

  public LoginAudit(String username, String ipAddress, boolean success, String remarks) {
    this.username = username;
    this.ipAddress = ipAddress;
    this.success = success;
    this.remarks = remarks;
    this.loginTime = LocalDateTime.now();
  }

  // --- Helpers for JSP Rendering ---
  public String getFormattedLoginTime() {
    if (loginTime == null) return "-";
    return loginTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
  }

  // --- Getters (the pages read the values; no setters needed) ---
  public Long getId() { return id; }

  public String getUsername() { return username; }

  public String getIpAddress() { return ipAddress; }

  public boolean isSuccess() { return success; }

  public String getRemarks() { return remarks; }

  public LocalDateTime getLoginTime() { return loginTime; }
}

