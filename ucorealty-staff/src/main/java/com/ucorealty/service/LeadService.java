package com.ucorealty.service;

import com.ucorealty.entity.Lead;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/** Single place where a customer lead is validated and stored. Same rules as the public portal, except that staff browsing on the intranet may reference any existing project. */
@Service
public class LeadService {
  public static final String STATUS_VISITOR = "Visitor";
  public static final String STATUS_INTEREST = "Showed interest in Home Loan";
  // a person's name: letters (any script), spaces, dots, apostrophes and hyphens; no markup
  private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M} .'\\-]{0,99}$");
  private static final Pattern MOBILE = Pattern.compile("^[6-9]\\d{9}$");

  private final LeadRepository leads;
  private final ProjectRepository projects;

  public LeadService(LeadRepository leads, ProjectRepository projects) {
    this.leads = leads;
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
    if (!NAME.matcher(cleanName).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter your name (letters, spaces, . ' - only, up to 100 characters)");
    }
    if (!MOBILE.matcher(cleanPhone).matches()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "10-digit mobile number required");
    }
    String normalized = normalizeStatus(status);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lead status");
    }
    String branch = (branchSelected == null || branchSelected.trim().isEmpty()) ? null : branchSelected.trim();
    if (branch != null && (branch.length() > 100 || branch.indexOf('<') >= 0 || branch.indexOf('>') >= 0)) {
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

    return l;
  }
}
