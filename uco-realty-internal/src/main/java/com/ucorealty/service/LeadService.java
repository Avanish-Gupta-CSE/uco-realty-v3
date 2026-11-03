package com.ucorealty.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucorealty.entity.Lead;
import com.ucorealty.entity.LeadTransferLog;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.LeadTransferLogRepository;
import com.ucorealty.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Single place where a customer lead is validated and stored. Same rules as the public portal, except that staff browsing on the intranet may reference any existing project. */
@Service
public class LeadService {
  public static final String STATUS_VISITOR = "Visitor";
  public static final String STATUS_INTEREST = "Showed interest in Home Loan";
  private static final Pattern MOBILE = Pattern.compile("^[6-9]\\d{9}$");
  private static final ObjectMapper JSON = new ObjectMapper();

  private final LeadRepository leads;
  private final LeadTransferLogRepository transfers;
  private final ProjectRepository projects;

  public LeadService(LeadRepository leads, LeadTransferLogRepository transfers, ProjectRepository projects) {
    this.leads = leads;
    this.transfers = transfers;
    this.projects = projects;
  }

  /** Maps any accepted spelling to one of the two stored statuses, or null when unknown. */
  public static String normalizeStatus(String status) {
    if (status == null || status.trim().isEmpty()) return STATUS_INTEREST;
    String s = status.trim();
    if ("VISITOR".equalsIgnoreCase(s)) return STATUS_VISITOR;
    if ("SUCCESSFUL_APPLIED".equalsIgnoreCase(s) || STATUS_INTEREST.equalsIgnoreCase(s)) return STATUS_INTEREST;
    return null;
  }

  @Transactional
  public Lead capture(String name, String phone, String projectId, String branchSelected, String status) {
    String cleanName = name == null ? "" : name.trim();
    String cleanPhone = phone == null ? "" : phone.trim();
    if (cleanName.isEmpty() || cleanName.length() > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A name of up to 100 characters is required");
    }
    if (!MOBILE.matcher(cleanPhone).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "10-digit mobile number required");
    }
    String normalized = normalizeStatus(status);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lead status");
    }
    String branch = (branchSelected == null || branchSelected.trim().isEmpty()) ? null : branchSelected.trim();
    if (branch != null && branch.length() > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is too long");
    }

    String pid = (projectId == null || projectId.trim().isEmpty()) ? null : projectId.trim();
    if (pid != null && !projects.existsById(pid)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
    }

    Lead l = new Lead();
    l.customerName = cleanName;
    l.phone = cleanPhone;
    l.projectId = pid;
    l.branchSelected = branch;
    l.status = normalized;
    leads.save(l);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", l.customerName);
    payload.put("phone", l.phone);
    payload.put("projectId", l.projectId);
    payload.put("branch", l.branchSelected);
    LeadTransferLog t = new LeadTransferLog();
    t.leadId = l.id;
    try {
      t.payload = JSON.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not build lead payload", e);
    }
    transfers.save(t);
    return l;
  }
}
