package com.ucorealty.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

// Read-only view of a `projects` row for the customer portal: only the columns customers may see.
// Authoring, approval and the internal workflow columns live in UcoRealtyStaff.
@Entity
@Table(name = "realty_projects")
public class Project {
  @Id
  @Column(name = "id", length = 20)
  public String id;

  @Column(name = "zone_name")
  public String zoneName;

  @Column(name = "branch_name")
  public String branchName;

  @Column(name = "state_name")
  public String stateName;

  @Column(name = "city_name")
  public String cityName;

  @Column(name = "project_name", nullable = false)
  public String projectName;

  @Column(name = "rera_no")
  public String reraNo;

  @Column(name = "property_id")
  public String propertyId;

  @Column(name = "type_of_unit")
  public String typeOfUnit;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "approval_date")
  public LocalDate approvalDate;

  // --- Inventory & price ---
  @Column(name = "total_inventory")
  public Integer totalInventory;

  @Column(name = "unsold_inventory")
  public Integer unsoldInventory;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "inventory_as_on_date")
  public LocalDate inventoryAsOnDate;

  @Lob
  @Column(name = "amenities")
  public String amenities;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "start_date")
  public LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "completion_date")
  public LocalDate completionDate;

  @Column(name = "apt_per_floor")
  public Integer aptPerFloor;

  @Column(name = "no_of_towers")
  public Integer noOfTowers;

  @Column(name = "total_project_area")
  public String totalProjectArea;

  @Column(name = "carpet_min")
  public String carpetMin;

  @Column(name = "carpet_max")
  public String carpetMax;

  @Column(name = "buildup_min")
  public String buildupMin;

  @Column(name = "buildup_max")
  public String buildupMax;

  @Column(name = "dist_railway")
  public String distRailway;

  @Column(name = "dist_airport")
  public String distAirport;

  @Column(name = "avg_rate")
  public String avgRate;

  @Column(name = "budget_min")
  public String budgetMin;

  @Column(name = "budget_max")
  public String budgetMax;

  // --- Builder & contacts ---
  @Column(name = "builder_name")
  public String builderName;

  @Column(name = "project_address", length = 500)
  public String projectAddress;

  @Column(name = "pin_code", length = 10)
  public String pinCode;

  @Column(name = "promoter1")
  public String promoter1;

  @Column(name = "promoter2")
  public String promoter2;

  @Column(name = "promoter3")
  public String promoter3;

  @Column(name = "promoter4")
  public String promoter4;

  @Column(name = "promoter5")
  public String promoter5;

  @Column(name = "sales_exec_name")
  public String salesExecName;

  @Column(name = "sales_mobile", length = 15)
  public String salesMobile;

  @Column(name = "sales_email")
  public String salesEmail;

  @Column(name = "website")
  public String website;

  @Column(name = "builder_rating", length = 50)
  public String builderRating;

  @Column(name = "builder_rating_out_of", length = 20)
  public String builderRatingOutOf;

  @Column(name = "video_url", length = 500)
  public String videoUrl;

  @Column(name = "amenity_flags", length = 600)
  public String amenityFlags;

  @Lob
  @Column(name = "builder_bg")
  public String builderBg;

  @Column(name = "branch_head_name")
  public String branchHeadName;

  @Column(name = "branch_head_number", length = 15)
  public String branchHeadNumber;

  @Column(name = "rel_exec_name")
  public String relExecName;

  @Column(name = "rel_exec_number", length = 15)
  public String relExecNumber;

  // --- Workflow: DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, NEEDS_CORRECTION ---
  @Column(name = "status", nullable = false)
  public String status = "DRAFT";

  @Column(name = "is_active")
  public Integer isActive = 1;

  @Column(name = "is_published")
  public Integer isPublished = 0;

  // --- Getters (the pages read the values through these; the database layer fills the fields directly, so no setters) ---
  public String getId() { return id; }
  public String getZoneName() { return zoneName; }
  public String getBranchName() { return branchName; }
  public String getStateName() { return stateName; }
  public String getCityName() { return cityName; }
  public String getProjectName() { return projectName; }
  public String getReraNo() { return reraNo; }
  public String getPropertyId() { return propertyId; }
  public String getTypeOfUnit() { return typeOfUnit; }
  public LocalDate getApprovalDate() { return approvalDate; }
  public Integer getTotalInventory() { return totalInventory; }
  public Integer getUnsoldInventory() { return unsoldInventory; }
  public LocalDate getInventoryAsOnDate() { return inventoryAsOnDate; }
  public String getAmenities() { return amenities; }
  public LocalDate getStartDate() { return startDate; }
  public LocalDate getCompletionDate() { return completionDate; }
  public Integer getAptPerFloor() { return aptPerFloor; }
  public Integer getNoOfTowers() { return noOfTowers; }
  public String getTotalProjectArea() { return totalProjectArea; }
  public String getCarpetMin() { return carpetMin; }
  public String getCarpetMax() { return carpetMax; }
  public String getBuildupMin() { return buildupMin; }
  public String getBuildupMax() { return buildupMax; }
  public String getDistRailway() { return distRailway; }
  public String getDistAirport() { return distAirport; }
  public String getAvgRate() { return avgRate; }
  public String getBudgetMin() { return budgetMin; }
  public String getBudgetMax() { return budgetMax; }
  public String getBuilderName() { return builderName; }
  public String getProjectAddress() { return projectAddress; }
  public String getPinCode() { return pinCode; }
  public String getPromoter1() { return promoter1; }
  public String getPromoter2() { return promoter2; }
  public String getPromoter3() { return promoter3; }
  public String getPromoter4() { return promoter4; }
  public String getPromoter5() { return promoter5; }
  public String getSalesExecName() { return salesExecName; }
  public String getSalesMobile() { return salesMobile; }
  public String getSalesEmail() { return salesEmail; }
  public String getWebsite() { return website; }
  public String getBuilderRating() { return builderRating; }
  public String getBuilderRatingOutOf() { return builderRatingOutOf; }
  public String getVideoUrl() { return videoUrl; }
  public String getAmenityFlags() { return amenityFlags; }
  public String getBuilderBg() { return builderBg; }
  public String getBranchHeadName() { return branchHeadName; }
  public String getBranchHeadNumber() { return branchHeadNumber; }
  public String getRelExecName() { return relExecName; }
  public String getRelExecNumber() { return relExecNumber; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getIsActive() { return isActive; }
  public Integer getActive() { return isActive; }
  public Integer getIsPublished() { return isPublished; }
  public Integer getPublished() { return isPublished; }
}
