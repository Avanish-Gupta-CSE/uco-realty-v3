package com.ucorealty.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global Exception Handler for UCO Bank Realty Internal Portal.
 * Formatted and aligned with reference projects (DelhiHighCourt, BranchRecordMaintainance).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public String handleMaxSizeException(MaxUploadSizeExceededException exc, Model model) {
    log.error("File upload size limit exceeded: {}", exc.getMessage());
    model.addAttribute("errorMessage", "File upload failed: Maximum file size allowed is 2MB per photo.");
    return "error";
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public String handleIllegalArgument(IllegalArgumentException exc, Model model) {
    log.error("Invalid argument exception: {}", exc.getMessage());
    model.addAttribute("errorMessage", exc.getMessage());
    return "error";
  }

  @ExceptionHandler(SecurityException.class)
  public String handleSecurityException(SecurityException exc, Model model) {
    log.error("Security exception: {}", exc.getMessage());
    model.addAttribute("errorMessage", "Access Denied: " + exc.getMessage());
    return "error";
  }

  @ExceptionHandler(Exception.class)
  public String handleGeneralException(Exception exc, Model model) {
    log.error("Unhandled exception caught by global handler: {}", exc.getMessage(), exc);
    model.addAttribute("errorMessage", "An unexpected system error occurred. Please contact the SWDC Cell or Bank Administrator.");
    return "error";
  }
}

