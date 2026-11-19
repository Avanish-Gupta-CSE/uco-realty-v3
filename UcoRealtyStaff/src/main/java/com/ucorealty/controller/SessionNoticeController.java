package com.ucorealty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Three static notice pages that replace the redirect to /login (the login page is only for testing; people normally arrive
 * through the bank portal at /entry): the session has expired, nobody is signed in, and the user signed out.
 * They are open to everyone, hold no data and create no session.
 * Change this file when: the wording or the page address of one of these notices changes (the page itself is session-notice.jsp).
 */
@Controller
public class SessionNoticeController {

  @GetMapping("/session-expired")
  public String sessionExpired(Model model) {
    return notice(model, "expired", "Session expired",
        "Your session has expired because of inactivity. Please sign in again from the UCO Bank intranet portal.");
  }

  @GetMapping("/not-signed-in")
  public String notSignedIn(Model model) {
    return notice(model, "not-signed-in", "You are not signed in",
        "Please sign in from the UCO Bank intranet portal to open the Realty portal.");
  }

  @GetMapping("/signed-out")
  public String signedOut(Model model) {
    return notice(model, "signed-out", "Signed out successfully",
        "You have been signed out. To use the Realty portal again, open it from the UCO Bank intranet portal.");
  }

  private String notice(Model model, String kind, String title, String message) {
    model.addAttribute("noticeKind", kind);
    model.addAttribute("noticeTitle", title);
    model.addAttribute("noticeMessage", message);
    return "session-notice";
  }
}
