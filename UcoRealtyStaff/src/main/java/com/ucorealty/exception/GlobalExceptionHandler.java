package com.ucorealty.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Answers every error of the staff app with the right status (401/403/404/400/405) and an error page, and writes one log
 * line per error ([UCO HTTP nnn] for expected ones, [UCO ERROR] for bugs).
 * Change this file when: an error page or an error log line should change.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleMaxSizeException(MaxUploadSizeExceededException exc, Model model) {
    log.error("File upload size limit exceeded: {}", exc.getMessage());
    model.addAttribute("errorMessage", "File upload failed: A photo can be at most 1 MB and a PDF document at most 3 MB.");
    return "error";
  }

  // Two people (or two clicks) changed the same request at the same moment: the second one is told, it is not a system error
  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
  public String handleSimultaneousChange(Exception exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(409);
    log.warn("[UCO HTTP 409] {} {} by {}: the request was changed at the same moment", request.getMethod(), request.getRequestURI(), who(request));
    model.addAttribute("errorMessage", "This request was just changed by another action (for example a second click). Please open the Projects list and check its current status.");
    return "error";
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public String handleIllegalArgument(IllegalArgumentException exc, Model model) {
    log.error("Invalid argument exception: {}", exc.getMessage());
    model.addAttribute("errorMessage", "The request was not valid. Please go back and try again.");
    return "error";
  }

  @ExceptionHandler(SecurityException.class)
  public String handleSecurityException(SecurityException exc, Model model) {
    log.error("Security exception: {}", exc.getMessage());
    model.addAttribute("errorMessage", "Access Denied.");
    return "error";
  }

  // Keep the real HTTP status (401/403/404/400) of StaffRoles and service rules instead of a 200 error page
  @ExceptionHandler(ResponseStatusException.class)
  public String handleStatus(ResponseStatusException exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    int code = exc.getStatusCode().value();
    response.setStatus(code);
    log.warn("[UCO HTTP {}] {} {} by {}: {}", code, request.getMethod(), request.getRequestURI(), who(request), exc.getReason());
    model.addAttribute("errorMessage", code == 403 ? "Access Denied: your role cannot open this page."
        : code == 404 ? "The requested item was not found."
        : code == 401 ? "Please sign in through the UCO Bank portal."
        : (exc.getReason() != null ? exc.getReason() : "The request could not be processed."));
    return "error";
  }

  // A URL that does not exist: a plain 404 page, one log line, no stack trace
  @ExceptionHandler(NoResourceFoundException.class)
  public String handleNotFound(NoResourceFoundException exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(404);
    log.warn("[UCO HTTP 404] {} {} by {}: no such page", request.getMethod(), request.getRequestURI(), who(request));
    model.addAttribute("errorMessage", "The page you asked for was not found.");
    return "error";
  }

  // A bad value in the address or form (for example a letter where a number is expected): 400, not a system error
  @ExceptionHandler({MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class})
  public String handleBadRequest(Exception exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(400);
    log.warn("[UCO HTTP 400] {} {} by {}: {}", request.getMethod(), request.getRequestURI(), who(request), exc.getMessage());
    model.addAttribute("errorMessage", "The request was not valid. Please go back and try again.");
    return "error";
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public String handleWrongMethod(HttpRequestMethodNotSupportedException exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(405);
    log.warn("[UCO HTTP 405] {} {} by {}: {}", request.getMethod(), request.getRequestURI(), who(request), exc.getMessage());
    model.addAttribute("errorMessage", "This page cannot be opened this way. Please use the buttons on the portal.");
    return "error";
  }

  @ExceptionHandler(Exception.class)
  public String handleGeneralException(Exception exc, HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(500);   // a real system error must not look like a successful page (200) to monitoring
    log.error("[UCO ERROR] Unhandled exception on {} {} by {}: {}", request.getMethod(), request.getRequestURI(), who(request), exc.getMessage(), exc);
    model.addAttribute("errorMessage", "An unexpected system error occurred. Please contact the SWDC Cell or Bank Administrator.");
    return "error";
  }

  private static String who(HttpServletRequest request) {
    return request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName();
  }
}

