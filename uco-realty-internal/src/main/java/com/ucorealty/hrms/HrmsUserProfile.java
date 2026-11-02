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
   * Resolves UCO Realty RBAC Role based on SOL ID and Cadre / Scale:
   * Aligned with bank logic:
   * - SOL in [7100..7199]: Head Office
   *     SOL 7114 / IT -> SUPERADMIN_DIT
   *     SOL 7105 / RETAIL -> HO_RETAIL
   *     SOL 7102 / MARKETING -> HO_MARKETING
   *     Other 71xx -> HO_ADMIN
   * - SOL in [7001..7099]: Zonal Office
   *     Scale >= 4 or Cadre EXEC -> ZO_CHECKER
   *     Scale <= 3 or Cadre OFFICER -> ZO_MAKER
   * - Branch SOLs:
   *     Scale >= 4 -> ZO_CHECKER; Scale <= 3 -> ZO_MAKER
   */
  public String getResolvedRole() {
    Long solNum = parseSolNumber(postingSolId);

    // Head Office (7100 to 7199)
    if (solNum != null && solNum >= 7100 && solNum < 7200) {
      if (solNum == 7114L || ("Head Office".equalsIgnoreCase(postingRegionName) && "IT".equalsIgnoreCase(jobProfile))
          || (tenureAs != null && tenureAs.toUpperCase().contains("IT"))) {
        return "SUPERADMIN_DIT";
      }
      if (solNum == 7105L || "RETAIL".equalsIgnoreCase(jobProfile)
          || (postingSolDesc != null && postingSolDesc.toUpperCase().contains("RETAIL"))) {
        return "HO_RETAIL";
      }
      if (solNum == 7102L || "MARKETING".equalsIgnoreCase(jobProfile)
          || (postingSolDesc != null && postingSolDesc.toUpperCase().contains("MARKETING"))) {
        return "HO_MARKETING";
      }
      return "HO_ADMIN";
    }

    // Zonal Office (7001 to 7099)
    if (solNum != null && solNum > 7000 && solNum < 7100) {
      int scale = parseScale(scaleCode);
      if (scale >= 4 || "EXEC".equalsIgnoreCase(cadre) || "EXECUTIVE".equalsIgnoreCase(cadre) || "EX".equalsIgnoreCase(cadre)) {
        return "ZO_CHECKER";
      } else {
        return "ZO_MAKER";
      }
    }

    // Default by scale
    int scale = parseScale(scaleCode);
    return scale >= 4 ? "ZO_CHECKER" : "ZO_MAKER";
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
    if (code == null) return 1;
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
    return 1;
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

