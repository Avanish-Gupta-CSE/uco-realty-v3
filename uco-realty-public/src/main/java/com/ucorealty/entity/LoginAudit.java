package com.ucorealty.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "login_audit")
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

  // --- Getters and Setters ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getIpAddress() { return ipAddress; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

  public boolean isSuccess() { return success; }
  public void setSuccess(boolean success) { this.success = success; }

  public String getRemarks() { return remarks; }
  public void setRemarks(String remarks) { this.remarks = remarks; }

  public LocalDateTime getLoginTime() { return loginTime; }
  public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}

