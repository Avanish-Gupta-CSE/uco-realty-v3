package com.ucorealty.dto;

import jakarta.validation.constraints.*;

public class ChangePasswordRequest {
  @NotBlank @Size(max = 72) public String currentPassword;
  @NotBlank @Size(min = 8, max = 72) public String newPassword;
}
