package com.ucorealty.security;

import com.ucorealty.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Reads the signed-in employee (role, zone) from the session. There is no user table to look up. */
public final class CurrentUser {
  private CurrentUser() {}

  /** The employee for this request, only if it is the one named (null otherwise). */
  public static AppUser find(String username) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AppUser user && user.getUsername().equals(username)) {
      return user;
    }
    return null;
  }

  public static AppUser of(Authentication auth) {
    return auth != null && auth.getPrincipal() instanceof AppUser user ? user : null;
  }
}
