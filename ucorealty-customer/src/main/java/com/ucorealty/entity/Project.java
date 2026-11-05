package com.ucorealty.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

// Read-only view of a `projects` row for the customer portal: only the columns customers may see.
// Authoring, approval and the internal workflow columns live in ucorealty-staff.
@Entity
@Table(name = "projects")
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

  // --- JavaBean Getters & Setters for JSP EL support ---
  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getZoneName() { return zoneName; }
  public void setZoneName(String zoneName) { this.zoneName = zoneName; }
  public String getBranchName() { return branchName; }
  public void setBranchName(String branchName) { this.branchName = branchName; }
  public String getStateName() { return stateName; }
  public void setStateName(String stateName) { this.stateName = stateName; }
  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }
  public String getProjectName() { return projectName; }
  public void setProjectName(String projectName) { this.projectName = projectName; }
  public String getReraNo() { return reraNo; }
  public void setReraNo(String reraNo) { this.reraNo = reraNo; }
  public String getPropertyId() { return propertyId; }
  public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
  public String getTypeOfUnit() { return typeOfUnit; }
  public void setTypeOfUnit(String typeOfUnit) { this.typeOfUnit = typeOfUnit; }
  public LocalDate getApprovalDate() { return approvalDate; }
  public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }
  public Integer getTotalInventory() { return totalInventory; }
  public void setTotalInventory(Integer totalInventory) { this.totalInventory = totalInventory; }
  public Integer getUnsoldInventory() { return unsoldInventory; }
  public void setUnsoldInventory(Integer unsoldInventory) { this.unsoldInventory = unsoldInventory; }
  public LocalDate getInventoryAsOnDate() { return inventoryAsOnDate; }
  public void setInventoryAsOnDate(LocalDate inventoryAsOnDate) { this.inventoryAsOnDate = inventoryAsOnDate; }
  public String getAmenities() { return amenities; }
  public void setAmenities(String amenities) { this.amenities = amenities; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getCompletionDate() { return completionDate; }
  public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }
  public Integer getAptPerFloor() { return aptPerFloor; }
  public void setAptPerFloor(Integer aptPerFloor) { this.aptPerFloor = aptPerFloor; }
  public Integer getNoOfTowers() { return noOfTowers; }
  public void setNoOfTowers(Integer noOfTowers) { this.noOfTowers = noOfTowers; }
  public String getTotalProjectArea() { return totalProjectArea; }
  public void setTotalProjectArea(String totalProjectArea) { this.totalProjectArea = totalProjectArea; }
  public String getCarpetMin() { return carpetMin; }
  public void setCarpetMin(String carpetMin) { this.carpetMin = carpetMin; }
  public String getCarpetMax() { return carpetMax; }
  public void setCarpetMax(String carpetMax) { this.carpetMax = carpetMax; }
  public String getBuildupMin() { return buildupMin; }
  public void setBuildupMin(String buildupMin) { this.buildupMin = buildupMin; }
  public String getBuildupMax() { return buildupMax; }
  public void setBuildupMax(String buildupMax) { this.buildupMax = buildupMax; }
  public String getDistRailway() { return distRailway; }
  public void setDistRailway(String distRailway) { this.distRailway = distRailway; }
  public String getDistAirport() { return distAirport; }
  public void setDistAirport(String distAirport) { this.distAirport = distAirport; }
  public String getAvgRate() { return avgRate; }
  public void setAvgRate(String avgRate) { this.avgRate = avgRate; }
  public String getBudgetMin() { return budgetMin; }
  public void setBudgetMin(String budgetMin) { this.budgetMin = budgetMin; }
  public String getBudgetMax() { return budgetMax; }
  public void setBudgetMax(String budgetMax) { this.budgetMax = budgetMax; }
  public String getBuilderName() { return builderName; }
  public void setBuilderName(String builderName) { this.builderName = builderName; }
  public String getProjectAddress() { return projectAddress; }
  public void setProjectAddress(String projectAddress) { this.projectAddress = projectAddress; }
  public String getPinCode() { return pinCode; }
  public void setPinCode(String pinCode) { this.pinCode = pinCode; }
  public String getPromoter1() { return promoter1; }
  public void setPromoter1(String promoter1) { this.promoter1 = promoter1; }
  public String getPromoter2() { return promoter2; }
  public void setPromoter2(String promoter2) { this.promoter2 = promoter2; }
  public String getPromoter3() { return promoter3; }
  public void setPromoter3(String promoter3) { this.promoter3 = promoter3; }
  public String getPromoter4() { return promoter4; }
  public void setPromoter4(String promoter4) { this.promoter4 = promoter4; }
  public String getPromoter5() { return promoter5; }
  public void setPromoter5(String promoter5) { this.promoter5 = promoter5; }
  public String getSalesExecName() { return salesExecName; }
  public void setSalesExecName(String salesExecName) { this.salesExecName = salesExecName; }
  public String getSalesMobile() { return salesMobile; }
  public void setSalesMobile(String salesMobile) { this.salesMobile = salesMobile; }
  public String getSalesEmail() { return salesEmail; }
  public void setSalesEmail(String salesEmail) { this.salesEmail = salesEmail; }
  public String getWebsite() { return website; }
  public void setWebsite(String website) { this.website = website; }
  public String getBuilderRating() { return builderRating; }
  public void setBuilderRating(String builderRating) { this.builderRating = builderRating; }
  public String getBuilderBg() { return builderBg; }
  public void setBuilderBg(String builderBg) { this.builderBg = builderBg; }
  public String getBranchHeadName() { return branchHeadName; }
  public void setBranchHeadName(String branchHeadName) { this.branchHeadName = branchHeadName; }
  public String getBranchHeadNumber() { return branchHeadNumber; }
  public void setBranchHeadNumber(String branchHeadNumber) { this.branchHeadNumber = branchHeadNumber; }
  public String getRelExecName() { return relExecName; }
  public void setRelExecName(String relExecName) { this.relExecName = relExecName; }
  public String getRelExecNumber() { return relExecNumber; }
  public void setRelExecNumber(String relExecNumber) { this.relExecNumber = relExecNumber; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getIsActive() { return isActive; }
  public void setIsActive(Integer isActive) { this.isActive = isActive; }
  public Integer getActive() { return isActive; }
  public void setActive(Integer active) { this.isActive = active; }
  public Integer getIsPublished() { return isPublished; }
  public void setIsPublished(Integer isPublished) { this.isPublished = isPublished; }
  public Integer getPublished() { return isPublished; }
  public void setPublished(Integer published) { this.isPublished = published; }
}
