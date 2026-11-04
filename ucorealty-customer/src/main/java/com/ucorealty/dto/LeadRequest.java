package com.ucorealty.dto;

import jakarta.validation.constraints.*;

// DTO = props interface for API. Never expose internal fields (createdBy/reviewedBy) to public.
public class LeadRequest {
  @NotBlank @Size(max = 100) public String name;
  @Pattern(regexp = "^[6-9]\\d{9}$", message = "10-digit mobile required")
  public String phone;
  @Size(max = 20) public String projectId;
  @Size(max = 100) public String branchSelected;
  @Size(max = 30) public String status;
}
