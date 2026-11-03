package com.ucorealty.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

  @ExceptionHandler(Exception.class)
  public String handleGeneralException(Exception exc, Model model) {
    log.error("Unhandled public portal exception: {}", exc.getMessage(), exc);
    model.addAttribute("errorMessage", "An error occurred while loading projects. Please try again.");
    return "public-projects";
  }
}
