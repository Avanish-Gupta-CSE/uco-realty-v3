package com.ucorealty.util;

import com.ucorealty.model.StaffUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the signed-in employee (role, zone) from the session. There is no user table to look up. */
public final class CurrentUser {
  private CurrentUser() {}

  /** The employee for this request, only if it is the one named (null otherwise). */
  public static StaffUser find(String username) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof StaffUser user && user.getUsername().equals(username)) {
      return user;
    }
    return null;
  }

  public static StaffUser of(Authentication auth) {
    return auth != null && auth.getPrincipal() instanceof StaffUser user ? user : null;
  }
}
