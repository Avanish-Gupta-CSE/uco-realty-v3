package com.ucorealty.entity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/**
 * The signed-in employee as the application sees them. It is NOT a table: it is built once at login from the HRMS
 * posting (see HrmsService.toAppUser) and Spring Security keeps it in the session as the "principal".
 * It extends Spring's User so that Spring knows the employee number and the role (ROLE_ + role) and nothing else is needed.
 * Change this file when: a new fact about the signed-in employee must be available on every request.
 */
public class AppUser extends User {
  private static final long serialVersionUID = 1L;

  public String username;   // employee number (EMPNO)
  public String role;       // SUPERADMIN_DIT, HO_MARKETING, HO_RETAIL, ZO_MAKER, ZO_CHECKER
  public String zone;       // for zonal staff: the official zone name; null for Head Office
  public String branch;     // posting description, e.g. "PATNA ZONAL OFFICE"

  public AppUser(String username, String role, String zone, String branch) {
    super(username, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
    this.username = username;
    this.role = role;
    this.zone = zone;
    this.branch = branch;
  }

  // Getters so JSP expressions can read the fields (getUsername() comes from Spring's User)
  public String getRole() { return role; }
  public String getZone() { return zone; }
  public String getBranch() { return branch; }
}
