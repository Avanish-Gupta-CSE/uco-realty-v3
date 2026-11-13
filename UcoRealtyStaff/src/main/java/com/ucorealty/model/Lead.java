package com.ucorealty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * One customer lead as the bank's Home Loan page saved it in the HRMS table LG_ENTRY_REVAMP (CHANNEL = 'REALTY').
 * READ ONLY: the Leads page and the Excel export read it; this application never writes to that table.
 * Change this file when: the Leads page needs one more column of the bank table (then also the SELECT in LgEntryRepository).
 */
@Entity
@Immutable
@Table(name = "LG_ENTRY_REVAMP")
public class Lead {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

  @Id
  @Column(name = "LEAD_ID")
  public String leadId;

  @Column(name = "REF_NAME")
  public String name;

  @Column(name = "MOBILE_NO")
  public String mobile;

  @Column(name = "EMAIL")
  public String email;

  @Column(name = "STATE")
  public String state;

  @Column(name = "DISTRICT")
  public String district;

  @Column(name = "FACILITY_INT")
  public String facility;      // e.g. HL = home loan

  @Column(name = "STATUS")
  public String status;        // shown exactly as the bank stores it (LEAD_STATUS is empty in the bank data)

  @Column(name = "TENT_LOAN_AMT")
  public BigDecimal tentativeLoanAmount;

  @Column(name = "LOANAMOUNT")
  public String loanAmountText;            // free text in the bank table

  @JsonIgnore
  @Column(name = "SUBMIT_DATE")
  private Timestamp submitDate;

  @JsonIgnore
  @Column(name = "CRE_DT")
  private Timestamp createdDate;

  /** The date of the lead: SUBMIT_DATE, or CRE_DT when the bank left that empty. */
  public Timestamp getSubmittedAt() {
    return submitDate != null ? submitDate : createdDate;
  }

  /** The loan amount to show: the numeric one when present, otherwise the text one. */
  public String getLoanAmount() {
    if (tentativeLoanAmount != null) return new DecimalFormat("#,##0.##").format(tentativeLoanAmount);
    return loanAmountText != null ? loanAmountText : "";
  }

  public String getFormattedDate() {
    Timestamp date = getSubmittedAt();
    return date == null ? "" : date.toLocalDateTime().format(DATE_FORMAT);
  }

  public String getLocation() {
    if (district != null && !district.isBlank() && state != null && !state.isBlank()) return district + ", " + state;
    return district != null && !district.isBlank() ? district : (state != null ? state : "");
  }

  // JSP expression language reads these through getters
  public String getLeadId() { return leadId; }
  public String getName() { return name; }
  public String getMobile() { return mobile; }
  public String getEmail() { return email; }
  public String getFacility() { return facility; }
  public String getStatus() { return status; }
}
