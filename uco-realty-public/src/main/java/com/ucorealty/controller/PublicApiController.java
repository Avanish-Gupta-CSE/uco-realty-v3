package com.ucorealty.controller;

import com.ucorealty.dto.LeadRequest;
import com.ucorealty.entity.Lead;
import com.ucorealty.entity.LeadTransferLog;
import com.ucorealty.entity.Project;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.LeadTransferLogRepository;
import com.ucorealty.repository.ProjectImageRepository;
import com.ucorealty.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

  private final ProjectService projects;
  private final LeadRepository leads;
  private final LeadTransferLogRepository transfers;
  private final ProjectImageRepository imageRepo;
  private static final ObjectMapper JSON = new ObjectMapper();

  public PublicApiController(ProjectService projects, LeadRepository leads,
                             LeadTransferLogRepository transfers, ProjectImageRepository imageRepo) {
    this.projects = projects;
    this.leads = leads;
    this.transfers = transfers;
    this.imageRepo = imageRepo;
  }

  @GetMapping("/projects")
  public Map<String, Object> list(
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "8") int size) {
    List<Map<String, Object>> all = projects.publicList(state, city, search).stream()
        .map(this::safeCard).collect(Collectors.toList());
    int total = all.size();
    int from = Math.min(page * size, total);
    int to = Math.min(from + size, total);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("total", total);
    out.put("page", page);
    out.put("items", all.subList(from, to));
    return out;
  }

  private Map<String, Object> safeCard(Project p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.id);
    m.put("projectName", p.projectName);
    m.put("builder", p.builderName);
    m.put("state", p.stateName);
    m.put("city", p.cityName);
    m.put("pinCode", p.pinCode);
    m.put("totalInventory", p.totalInventory);
    m.put("availableInventory", p.unsoldInventory);
    m.put("rera", p.reraNo);
    m.put("price", fmtPrice(p.budgetMin, p.budgetMax));
    m.put("typeOfUnit", p.typeOfUnit);
    return m;
  }

  private String fmtPrice(String min, String max) {
    if (min == null && max == null) return null;
    if (min != null && max != null) {
      if (min.startsWith("₹")) return min + " – " + max;
      return "₹ " + min + " – " + max;
    }
    return min != null ? min : max;
  }

  @GetMapping("/projects/{id}")
  public Map<String, Object> detail(@PathVariable String id) {
    Project p = projects.publicById(id);

    Map<String, Object> m = safeCard(p);
    m.put("zone", p.zoneName);
    m.put("branch", p.branchName);
    m.put("projectAddress", p.projectAddress);
    m.put("pinCode", p.pinCode);
    m.put("propertyId", p.propertyId);
    m.put("typeOfUnit", p.typeOfUnit);
    m.put("approvalDate", p.approvalDate);
    m.put("startDate", p.startDate);
    m.put("completionDate", p.completionDate);
    m.put("inventoryAsOnDate", p.inventoryAsOnDate);
    m.put("amenities", p.amenities);
    m.put("aptPerFloor", p.aptPerFloor);
    m.put("noOfTowers", p.noOfTowers);
    m.put("totalProjectArea", p.totalProjectArea);
    m.put("carpetMin", p.carpetMin);
    m.put("carpetMax", p.carpetMax);
    m.put("buildupMin", p.buildupMin);
    m.put("buildupMax", p.buildupMax);
    m.put("distRailway", p.distRailway);
    m.put("distAirport", p.distAirport);
    m.put("avgRate", p.avgRate);
    m.put("budgetMin", p.budgetMin);
    m.put("budgetMax", p.budgetMax);

    m.put("builderName", p.builderName);
    m.put("builderRating", p.builderRating);
    m.put("builderBg", p.builderBg);
    m.put("website", p.website);
    m.put("promoter1", p.promoter1);
    m.put("promoter2", p.promoter2);
    m.put("promoter3", p.promoter3);
    m.put("promoter4", p.promoter4);
    m.put("promoter5", p.promoter5);
    m.put("salesExecName", p.salesExecName);
    m.put("salesMobile", p.salesMobile);
    m.put("salesEmail", p.salesEmail);

    m.put("branchHeadName", p.branchHeadName);
    m.put("branchHeadNumber", p.branchHeadNumber);
    m.put("relExecName", p.relExecName);
    m.put("relExecNumber", p.relExecNumber);

    List<String> files = imageRepo.findByProjectId(id).stream()
        .filter(i -> i.mimeType != null && i.mimeType.startsWith("image/"))
        .map(i -> i.filePath).collect(Collectors.toList());
    m.put("images", files);
    return m;
  }

  @GetMapping("/meta")
  public Map<String, Object> meta() {
    List<Project> approved = projects.publicList(null, null, null);
    TreeMap<String, TreeSet<String>> stateCities = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    TreeSet<String> allCities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (Project p : approved) {
      if (p.stateName == null || p.stateName.trim().isEmpty()) continue;
      TreeSet<String> cities = stateCities.computeIfAbsent(p.stateName.trim(), k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
      if (p.cityName != null && !p.cityName.trim().isEmpty()) {
        cities.add(p.cityName.trim());
        allCities.add(p.cityName.trim());
      }
    }
    Map<String, List<String>> stateCitiesOut = new LinkedHashMap<>();
    stateCities.forEach((st, cs) -> stateCitiesOut.put(st, new ArrayList<>(cs)));

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("states", new ArrayList<>(stateCities.keySet()));
    m.put("cities", new ArrayList<>(allCities));
    m.put("stateCities", stateCitiesOut);
    return m;
  }

  @PostMapping("/leads")
  public Map<String, Object> createLead(@Valid @RequestBody LeadRequest req) {
    String status = (req.status == null || req.status.trim().isEmpty()) ? "Visitor" : req.status.trim();
    if ("VISITOR".equalsIgnoreCase(status) || "Visitor".equalsIgnoreCase(status)) {
      status = "Visitor";
    } else if ("SUCCESSFUL_APPLIED".equalsIgnoreCase(status) || "Showed interest in Home Loan".equalsIgnoreCase(status)) {
      status = "Showed interest in Home Loan";
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lead status");
    }
    String projectId = (req.projectId == null || req.projectId.trim().isEmpty()) ? null : req.projectId.trim();
    if ("Showed interest in Home Loan".equals(status)) {
      if (projectId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A project is required to apply");
      projects.publicById(projectId);
    } else {
      projectId = null;
    }

    Lead l = new Lead();
    l.customerName = req.name.trim();
    l.phone = req.phone;
    l.projectId = projectId;
    l.branchSelected = req.branchSelected;
    l.status = status;
    leads.save(l);

    LeadTransferLog t = new LeadTransferLog();
    t.leadId = l.id;
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", l.customerName);
    payload.put("phone", l.phone);
    payload.put("projectId", l.projectId);
    payload.put("branch", l.branchSelected);
    try {
      t.payload = JSON.writeValueAsString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Could not build lead payload", e);
    }
    transfers.save(t);

    Map<String, Object> res = new LinkedHashMap<>();
    res.put("leadId", l.id);
    res.put("message", "Thank you! Your Home Loan interest has been recorded. UCO Bank representative will contact you shortly.");
    return res;
  }
}
