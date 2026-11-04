package com.ucorealty.security;

import com.ucorealty.entity.AppUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/** What Spring Security keeps in the session for a signed-in employee: the employee number, the role and the zone. */
public class StaffPrincipal extends User {
  private static final long serialVersionUID = 1L;

  private final AppUser appUser;

  public StaffPrincipal(AppUser appUser) {
    super(appUser.username, "", Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.role)));
    this.appUser = appUser;
  }

  public AppUser getAppUser() { return appUser; }
}
