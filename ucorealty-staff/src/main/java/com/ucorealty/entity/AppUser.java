package com.ucorealty.entity;

import java.io.Serializable;

/**
 * The signed-in employee as the application sees them. It is NOT a table: it is built once at login from the HRMS
 * posting (see HrmsServiceImpl.toAppUser) and travels inside the session (security/StaffPrincipal).
 */
public class AppUser implements Serializable {
  private static final long serialVersionUID = 1L;

  public String username;   // employee number (EMPNO)
  public String role;       // SUPERADMIN_DIT, HO_MARKETING, HO_RETAIL, ZO_MAKER, ZO_CHECKER
  public String zone;       // for zonal staff: the official zone name; null for Head Office
  public String branch;     // posting description, e.g. "PATNA ZONAL OFFICE"

  public AppUser(String username, String role, String zone, String branch) {
    this.username = username;
    this.role = role;
    this.zone = zone;
    this.branch = branch;
  }

  // JavaBean getters so JSP expressions can read the fields
  public String getUsername() { return username; }
  public String getRole() { return role; }
  public String getZone() { return zone; }
  public String getBranch() { return branch; }
}
