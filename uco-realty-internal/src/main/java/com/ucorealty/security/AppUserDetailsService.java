package com.ucorealty.security;

import com.ucorealty.entity.AppUser;
import com.ucorealty.hrms.HrmsService;
import com.ucorealty.hrms.HrmsUserProfile;
import com.ucorealty.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AppUserDetailsService implements UserDetailsService {
  private final UserRepository users;
  private final HrmsService hrmsService;

  public AppUserDetailsService(UserRepository users, @Lazy HrmsService hrmsService) {
    this.users = users;
    this.hrmsService = hrmsService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AppUser u = users.findByUsername(username)
      .orElseGet(() -> {
        if (hrmsService != null) {
          HrmsUserProfile profile = hrmsService.fetchUserProfile(username);
          if (profile != null) {
            return hrmsService.syncAppUser(profile);
          }
        }
        return null;
      });

    if (u == null) {
      throw new UsernameNotFoundException("User not found: " + username);
    }
    if (u.isActive != 1) {
      throw new UsernameNotFoundException("Account deactivated: " + username);
    }

    return new User(
      u.username,
      u.passwordHash,
      Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.role))
    );
  }
}

