package com.ucorealty.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stops a request when the signed-in employee does not have one of the allowed roles (401 when nobody is signed in, 403 when the
 * role is not allowed). Use it first in every controller method: RoleCheck.require(auth, Roles.MAKERS).
 * Change this file when: the way a role is checked changes. (Which roles exist: Roles.)
 */
public class RoleCheck {
  public static String require(Authentication auth, String... roles) {
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
    for (GrantedAuthority granted : auth.getAuthorities())
      for (String r : roles)
        if (granted.getAuthority().equals("ROLE_" + r))
          return auth.getName();
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "role not allowed");
  }
}

