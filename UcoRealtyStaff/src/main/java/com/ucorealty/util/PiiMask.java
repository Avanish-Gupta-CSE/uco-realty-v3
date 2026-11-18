package com.ucorealty.util;

/**
 * Masks personal data of customers for the screen and the Excel file (secure coding order, point 5: PII is shown masked only).
 * Example: mobile 9876543210 -> 98XXXXXX10, e-mail rohit.sharma@mail.com -> ro****@mail.com, name Rohit Sharma -> R**** S*****.
 * Change this file when: the masking style changes. Whether masking is on is the setting uco.leads.mask-pii.
 */
public final class PiiMask {
  private PiiMask() {}

  public static String mobile(String mobile) {
    if (mobile == null) return "";
    String t = mobile.trim();
    int n = t.length();
    if (n <= 4) return "X".repeat(n);
    return t.substring(0, 2) + "X".repeat(n - 4) + t.substring(n - 2);
  }

  public static String email(String email) {
    if (email == null) return "";
    String t = email.trim();
    int at = t.indexOf('@');
    if (at <= 0) return t.isEmpty() ? "" : t.charAt(0) + "****";
    int keep = Math.min(2, at);
    return t.substring(0, keep) + "****" + t.substring(at);
  }

  public static String name(String name) {
    if (name == null) return "";
    StringBuilder out = new StringBuilder();
    for (String word : name.trim().split("\\s+")) {
      if (word.isEmpty()) continue;
      if (out.length() > 0) out.append(' ');
      out.append(word.charAt(0)).append("*".repeat(Math.min(word.length() - 1, 8)));
    }
    return out.toString();
  }
}
