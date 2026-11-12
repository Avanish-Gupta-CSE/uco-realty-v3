package com.ucorealty.util;

/**
 * The names of the BANK's tables that this application only reads, written once, with the database user that owns them.
 * Our own tables (realty_projects, ...) are written plainly in their repositories because they belong to our own database user.
 *
 * The bank's HRMS tables belong to the database user HRMS_DBA; our own database user has READ (SELECT) access to them,
 * which is why the owner name is part of the table name.
 *
 * Change this file when: a bank table moves to another database user, or is renamed. Edit the one line, rebuild, deploy.
 * (If the tables ever belong to our own user, remove "HRMS_DBA." from the line.)
 */
public final class DatabaseTables {
  private DatabaseTables() {}

  /** Who the employee is and where posted (sign-in, role, zone). */
  public static final String EMPLOYEE = "HRMS_DBA.employee_contact_details";

  /** The employee's current posting (overrides the posting in EMPLOYEE). */
  public static final String EMPLOYEE_TENURE = "HRMS_DBA.employee_tenure";

  /** The customer leads saved by the bank's Home Loan page (our leads are the rows with CHANNEL = 'REALTY'). */
  public static final String LEADS = "HRMS_DBA.LG_ENTRY_REVAMP";
}
