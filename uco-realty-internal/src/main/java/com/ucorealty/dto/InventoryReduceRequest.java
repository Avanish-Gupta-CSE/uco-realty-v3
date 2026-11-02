package com.ucorealty.dto;

import jakarta.validation.constraints.*;

public class InventoryReduceRequest {
  @NotNull @Min(1) @Max(100000) public Integer units;
  @NotBlank @Size(max = 300) public String reason;
}
