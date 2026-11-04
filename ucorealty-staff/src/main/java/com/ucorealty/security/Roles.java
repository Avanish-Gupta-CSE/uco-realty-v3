package com.ucorealty.security;

/** Role groups used by RoleCheck. A role is derived at login from the employee's HRMS posting (see RoleResolver). */
public final class Roles {
  private Roles() {}

  /** May create / change projects (a Zonal Maker for his zone; DIT and HO Marketing for every zone). */
  public static final String[] MAKERS = {"ZO_MAKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  /** May approve / reject / return requests (a Zonal Checker for his zone; DIT and HO Marketing for every zone). */
  public static final String[] CHECKERS = {"ZO_CHECKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  /** Every staff role (read access to the project directory). */
  public static final String[] ALL_STAFF = {"SUPERADMIN_DIT", "HO_MARKETING", "HO_RETAIL", "ZO_MAKER", "ZO_CHECKER"};
  /** Customer leads page and Excel export. */
  public static final String[] LEAD_ROLES = {"HO_MARKETING", "HO_RETAIL", "SUPERADMIN_DIT"};
}
