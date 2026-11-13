package com.ucorealty.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * One employee as the bank's HRMS table employee_contact_details knows them (only the columns we read).
 * READ ONLY: this application never writes to that table. The sign-in code may change the posting fields of its own copy
 * (the current posting row overrides them), but a changed copy is never saved.
 * Change this file when: we need one more column of the employee table.
 */
@Entity
@Immutable
@Table(name = "employee_contact_details")
public class HrmsEmployee {

  @Id
  @Column(name = "EMPNO")
  private String empNo;

  @Column(name = "NAME")
  private String name;

  @Column(name = "POSTING_SOL_ID")
  private String postingSolId;

  @Column(name = "POSTING_SOL_DESC")
  private String postingSolDesc;

  @Column(name = "SCALE_CODE")
  private String scaleCode;

  @Column(name = "CADRE_CODE")
  private String cadre;

  @Column(name = "POSTING_REGION_NAME")
  private String postingRegionName;

  @Column(name = "DESIG")
  private String tenureAs;

  @Column(name = "MOBILE_NO")
  private String mobile;

  @Column(name = "E_MAIL")
  private String email;

  @Column(name = "RETIRE_FLAG")
  private String retireFlag;

  public String getEmpNo() { return empNo; }
  public String getName() { return name; }
  public String getPostingSolId() { return postingSolId; }
  public String getPostingSolDesc() { return postingSolDesc; }
  public String getScaleCode() { return scaleCode; }
  public String getCadre() { return cadre; }
  public String getPostingRegionName() { return postingRegionName; }
  public String getTenureAs() { return tenureAs; }
  public String getMobile() { return mobile; }
  public String getEmail() { return email; }

  /** False for a retired employee (RETIRE_FLAG = 'RT'); retired employees may not sign in. */
  public boolean isActive() { return !"RT".equalsIgnoreCase(retireFlag); }

  // The current posting row (employee_tenure) overrides these three facts of the contact row
  public void setPostingSolId(String postingSolId) { this.postingSolId = postingSolId; }
  public void setScaleCode(String scaleCode) { this.scaleCode = scaleCode; }
  public void setTenureAs(String tenureAs) { this.tenureAs = tenureAs; }
}
