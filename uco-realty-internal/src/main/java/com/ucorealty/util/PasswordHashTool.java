package com.ucorealty.util;

import com.ucorealty.security.PasswordPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Command-line helper for DIT/DBA: prints the BCrypt hash to paste into 02_INITIAL_ADMIN_USERS.sql.
public class PasswordHashTool {
  public static void main(String[] args) {
    String pw;
    if (args.length > 0) {
      pw = args[0];
    } else if (System.console() != null) {
      pw = new String(System.console().readPassword("New password: "));
    } else {
      System.err.println("No console available. Pass the password as the first argument.");
      System.exit(2);
      return;
    }
    try {
      PasswordPolicy.check(pw, null);
    } catch (org.springframework.web.server.ResponseStatusException e) {
      System.err.println("Rejected: " + e.getReason());
      System.exit(1);
      return;
    }
    System.out.println(new BCryptPasswordEncoder().encode(pw));
  }
}

