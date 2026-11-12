package com.ucorealty.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * One customer lead as the bank's Home Loan page saved it in the HRMS table LG_ENTRY_REVAMP (CHANNEL = 'REALTY').
 * A plain data holder: LgEntryRepository fills it, the Leads page and the Excel export read it. Nothing here is stored by us.
 */
public class LeadRow {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

  public String leadId;
  public String name;
  public String mobile;
  public String email;
  public String state;
  public String district;
  public String facility;      // FACILITY_INT, e.g. HL = home loan
  public String status;        // STATUS column of the bank table, shown exactly as the bank stores it (LEAD_STATUS is empty in the bank data)
  public BigDecimal tentativeLoanAmount;   // TENT_LOAN_AMT
  public String loanAmountText;            // LOANAMOUNT (free text in the bank table)
  public Timestamp submittedAt;            // SUBMIT_DATE, or CRE_DT when that is empty

  /** The loan amount to show: the numeric one when present, otherwise the text one. */
  public String getLoanAmount() {
    if (tentativeLoanAmount != null) return new DecimalFormat("#,##0.##").format(tentativeLoanAmount);
    return loanAmountText != null ? loanAmountText : "";
  }

  public String getFormattedDate() {
    return submittedAt == null ? "" : submittedAt.toLocalDateTime().format(DATE_FORMAT);
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
