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

