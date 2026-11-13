package com.ucorealty.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every staff page (see StaffWebConfig), so no controller has to check the session itself:
 * a request passes only when the session holds emp_id and sol_id (set by HomeController at login); otherwise it goes to the login page.
 * /entry is not covered: that is where the session gets created.
 * Change this file when: the signed-in check changes (which session values must exist).
 */
@Component
public class SessionInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(SessionInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    HttpSession session = request.getSession(false);
    if (session != null && session.getAttribute("emp_id") != null && session.getAttribute("sol_id") != null) {
      return true;
    }
    log.warn("[UCO SSO] No signed-in session for {}: sent to the login page", request.getRequestURI());
    // The browser still sends a session cookie that the server no longer knows: the 30 minutes ran out, so say so on the login page
    boolean expired = request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid();
    response.sendRedirect(request.getContextPath() + "/login" + (expired ? "?expired" : ""));
    return false;
  }
}
