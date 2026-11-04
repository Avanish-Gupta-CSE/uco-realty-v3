package com.ucorealty.service;

import com.ucorealty.entity.Project;
import com.ucorealty.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only catalog for the internet-facing portal. Only APPROVED + active + published projects are ever
 * returned; everything else behaves as "not found". Project authoring/approval lives in ucorealty-staff.
 */
@Service
public class ProjectService {
  private final ProjectRepository repo;

  public ProjectService(ProjectRepository repo) {
    this.repo = repo;
  }

  private static boolean isVisibleToPublic(Project p) {
    return "APPROVED".equals(p.status)
      && Integer.valueOf(1).equals(p.isActive)
      && Integer.valueOf(1).equals(p.isPublished);
  }

  public Project publicById(String id) {
    return repo.findById(id).filter(ProjectService::isVisibleToPublic)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found or not published"));
  }

  public List<Project> publicList(String state, String city, String search) {
    List<Project> all = repo.findByStatusAndIsActiveAndIsPublished("APPROVED", 1, 1);
    return all.stream()
      .filter(p -> state == null || state.trim().isEmpty() || state.equalsIgnoreCase(p.stateName))
      .filter(p -> city == null || city.trim().isEmpty() || city.equalsIgnoreCase(p.cityName))
      .filter(p -> search == null || search.trim().isEmpty() ||
          (p.projectName != null && p.projectName.toLowerCase().contains(search.toLowerCase())) ||
          (p.builderName != null && p.builderName.toLowerCase().contains(search.toLowerCase())) ||
          (p.pinCode != null && p.pinCode.contains(search)) ||
          (p.reraNo != null && p.reraNo.toLowerCase().contains(search.toLowerCase())))
      .collect(Collectors.toList());
  }
}
