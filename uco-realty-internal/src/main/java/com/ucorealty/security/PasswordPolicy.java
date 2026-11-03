package com.ucorealty.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PasswordPolicy {
  private PasswordPolicy() {}

  public static void check(String password, String username) {
    if (password == null || password.length() < 8 || password.length() > 72) {
      throw bad("Password must be 8 to 72 characters");
    }
    boolean hasLetter = password.chars().anyMatch(Character::isLetter);
    boolean hasDigit = password.chars().anyMatch(Character::isDigit);
    if (!hasLetter || !hasDigit) {
      throw bad("Password must contain at least one letter and one digit");
    }
    if (username != null && password.equalsIgnoreCase(username)) {
      throw bad("Password must not be the same as the username");
    }
  }

  private static ResponseStatusException bad(String msg) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
  }
}

