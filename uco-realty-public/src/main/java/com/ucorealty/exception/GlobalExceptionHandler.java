package com.ucorealty.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Global Exception Handler for UCO Bank Realty Public Customer Portal.
 * Formatted and aligned with reference projects (DelhiHighCourt, BranchRecordMaintainance).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public String handleIllegalArgument(IllegalArgumentException exc, Model model) {
    log.error("Invalid public portal request: {}", exc.getMessage());
    model.addAttribute("errorMessage", exc.getMessage());
    return "public-projects";
  }

  // 404 for unknown/unpublished projects, 400 for bad requests: keep their real HTTP status instead of a 200 page.
  @ExceptionHandler(ResponseStatusException.class)
  public String handleStatus(ResponseStatusException exc, HttpServletResponse response, Model model) {
    response.setStatus(exc.getStatusCode().value());
    model.addAttribute("errorMessage", exc.getStatusCode().value() == 404
        ? "That project is not available." : "The request could not be processed.");
    return "public-projects";
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public String handleNoResource(HttpServletResponse response, Model model) {
    response.setStatus(404);
    model.addAttribute("errorMessage", "That page is not available.");
    return "public-projects";
  }

  @ExceptionHandler(Exception.class)
  public String handleGeneralException(Exception exc, Model model) {
    log.error("Unhandled public portal exception: {}", exc.getMessage(), exc);
    model.addAttribute("errorMessage", "An error occurred while loading projects. Please try again.");
    return "public-projects";
  }
}

