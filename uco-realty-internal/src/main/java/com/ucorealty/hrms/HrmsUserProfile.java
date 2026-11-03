package com.ucorealty.hrms;

import java.io.Serializable;

/**
 * UCO Bank Employee Profile Model aligned with bank table employee_contact_details
 * and reference projects BranchRecordMaintainance & ESG.
 */
public class HrmsUserProfile implements Serializable {
  private static final long serialVersionUID = 1L;

  private String empNo;
  private String name;
  private String postingSolId;
  private String postingSolDesc;
  private String scaleCode;
  private String cadre;
  private String postingRegionName;
  private String tenureAs;
  private String jobProfile;
  private String mobile;
  private String email;
  private boolean active = true;

  public HrmsUserProfile() {}

  public HrmsUserProfile(String empNo, String name, String postingSolId, String postingSolDesc,
                         String scaleCode, String cadre, String postingRegionName, String tenureAs) {
    this.empNo = empNo;
    this.name = name;
    this.postingSolId = postingSolId;
    this.postingSolDesc = postingSolDesc;
    this.scaleCode = scaleCode;
    this.cadre = cadre;
    this.postingRegionName = postingRegionName;
    this.tenureAs = tenureAs;
  }

  /**
   * Resolves the UCO Realty role from the HRMS posting. Only the SOL ranges below get any access;
   * every other posting (branches, regional offices, unknown SOL) resolves to null and is refused at login.
   * - SOL 7114 -> SUPERADMIN_DIT, 7105 -> HO_RETAIL, 7102 -> HO_MARKETING, other 71xx -> HO_ADMIN
   * - SOL 70xx (zonal office) with a known zone: Scale >= 4 or Cadre EXEC -> ZO_CHECKER, Scale 1-3 -> ZO_MAKER, scale 0/blank -> no access
   * Matching is by exact SOL / exact cadre; no substring guessing on free-text designations.
   */
  public String getResolvedRole() {
    Long solNum = parseSolNumber(postingSolId);
    if (solNum == null) return null;

    // Head Office (7100 to 7199)
    if (solNum >= 7100 && solNum < 7200) {
      if (solNum == 7114L) return "SUPERADMIN_DIT";
      if (solNum == 7105L) return "HO_RETAIL";
      if (solNum == 7102L) return "HO_MARKETING";
      return "HO_ADMIN";
    }

    // Zonal Office (7001 to 7099): zone is mandatory, a maker/checker without a zone would see nothing useful
    if (solNum > 7000 && solNum < 7100) {
      if (getResolvedZone() == null) return null;
      boolean exec = "EXEC".equalsIgnoreCase(cadre) || "EXECUTIVE".equalsIgnoreCase(cadre) || "EX".equalsIgnoreCase(cadre);
      int scale = parseScale(scaleCode);
      if (exec || scale >= 4) return "ZO_CHECKER";
      if (scale >= 1) return "ZO_MAKER";   // Scale 1-3 officers
      return null;                          // scale 0 / blank / unknown (clerical, subordinate staff): no access
    }

    return null;
  }

  /**
   * Resolves assigned Zone for Zonal Office employees (SOL 70xx).
   * Head office employees (SOL 71xx) return null (National / All Zones).
   */
  public String getResolvedZone() {
    Long solNum = parseSolNumber(postingSolId);
    if (solNum != null && solNum > 7000 && solNum < 7100) {
      if (postingRegionName != null && !postingRegionName.trim().isEmpty() && !"Head Office".equalsIgnoreCase(postingRegionName)) {
        return cleanZone(postingRegionName);
      }
      if (postingSolDesc != null && !postingSolDesc.trim().isEmpty()) {
        return cleanZone(postingSolDesc);
      }
    }
    return null;
  }

  private Long parseSolNumber(String sol) {
    if (sol == null) return null;
    String clean = sol.replaceAll("[^0-9]", "");
    if (clean.isEmpty()) return null;
    try {
      return Long.parseLong(clean);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private int parseScale(String code) {
    if (code == null || code.trim().isEmpty()) return 0;
    String clean = code.replaceAll("[^0-9]", "");
    if (!clean.isEmpty()) {
      try {
        return Integer.parseInt(clean);
      } catch (NumberFormatException ignored) {}
    }
    String upper = code.toUpperCase().trim();
    if (upper.contains("VII") || upper.endsWith("7")) return 7;
    if (upper.contains("VI") || upper.endsWith("6")) return 6;
    if (upper.contains("V") || upper.endsWith("5")) return 5;
    if (upper.contains("IV") || upper.endsWith("4")) return 4;
    if (upper.contains("III") || upper.endsWith("3")) return 3;
    if (upper.contains("II") || upper.endsWith("2")) return 2;
    if (upper.contains("I") || upper.endsWith("1")) return 1;
    return 0;
  }

  private String cleanZone(String raw) {
    if (raw == null) return null;
    String z = raw.replace("ZO", "").replace("ZONE", "").replace("Zonal Office", "").trim();
    if (z.contains("(")) {
      z = z.substring(0, z.indexOf('(')).trim();
    }
    return z.isEmpty() ? raw.trim() : z;
  }

  // --- Getters & Setters ---
  public String getEmpNo() { return empNo; }
  public void setEmpNo(String empNo) { this.empNo = empNo; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getPostingSolId() { return postingSolId; }
  public void setPostingSolId(String postingSolId) { this.postingSolId = postingSolId; }

  public String getPostingSolDesc() { return postingSolDesc; }
  public void setPostingSolDesc(String postingSolDesc) { this.postingSolDesc = postingSolDesc; }

  public String getScaleCode() { return scaleCode; }
  public void setScaleCode(String scaleCode) { this.scaleCode = scaleCode; }

  public String getCadre() { return cadre; }
  public void setCadre(String cadre) { this.cadre = cadre; }

  public String getPostingRegionName() { return postingRegionName; }
  public void setPostingRegionName(String postingRegionName) { this.postingRegionName = postingRegionName; }

  public String getTenureAs() { return tenureAs; }
  public void setTenureAs(String tenureAs) { this.tenureAs = tenureAs; }

  public String getJobProfile() { return jobProfile; }
  public void setJobProfile(String jobProfile) { this.jobProfile = jobProfile; }

  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}

