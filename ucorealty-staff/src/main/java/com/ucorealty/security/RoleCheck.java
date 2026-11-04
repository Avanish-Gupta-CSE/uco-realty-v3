package com.ucorealty.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

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

