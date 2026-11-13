package com.ucorealty.util;

import com.ucorealty.model.StaffUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Everything about WHO the signed-in employee is and WHAT the role allows, in one place.
 * A role is derived at login from the employee's HRMS posting (see RoleResolver); there is no user table to look up.
 *
 * 1. The role groups (MAKERS, CHECKERS, ...): which roles may use which kind of page.
 * 2. require(...): stops a request when the role is not allowed. Use it first in every controller method:
 *    StaffRoles.require(auth, StaffRoles.MAKERS).
 * 3. userOf / currentUser: the signed-in employee (role, zone) from the session.
 *
 * Change this file when: a role group changes (who may open a page) or the way a role is checked changes.
 */
public final class StaffRoles {
  private StaffRoles() {}

  /** May create / change projects (a Zonal Maker for his zone; DIT and HO Marketing for every zone). */
  public static final String[] MAKERS = {"ZO_MAKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  /** May approve / reject / return requests (a Zonal Checker for his zone; DIT and HO Marketing for every zone). */
  public static final String[] CHECKERS = {"ZO_CHECKER", "HO_MARKETING", "SUPERADMIN_DIT"};
  /** Every staff role (read access to the project directory). */
  public static final String[] ALL_STAFF = {"SUPERADMIN_DIT", "HO_MARKETING", "HO_RETAIL", "ZO_MAKER", "ZO_CHECKER"};
  /** Customer leads page and Excel export. */
  public static final String[] LEAD_ROLES = {"HO_MARKETING", "HO_RETAIL", "SUPERADMIN_DIT"};

  /**
   * Stops a request when the signed-in employee does not have one of the allowed roles (401 when nobody is signed in,
   * 403 when the role is not allowed). Returns the employee id when the role is allowed.
   */
  public static String require(Authentication auth, String... roles) {
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
    for (GrantedAuthority granted : auth.getAuthorities())
      for (String r : roles)
        if (granted.getAuthority().equals("ROLE_" + r))
          return auth.getName();
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role not allowed");
  }

  /** The employee of this request, only if it is the one named (null otherwise). */
  public static StaffUser currentUser(String username) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof StaffUser user && user.getUsername().equals(username)) {
      return user;
    }
    return null;
  }

  /** The employee behind this sign-in, or null when nobody is signed in. */
  public static StaffUser userOf(Authentication auth) {
    return auth != null && auth.getPrincipal() instanceof StaffUser user ? user : null;
  }
}
