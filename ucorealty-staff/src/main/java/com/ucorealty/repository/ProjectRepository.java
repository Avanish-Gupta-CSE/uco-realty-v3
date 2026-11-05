package com.ucorealty.repository;

import com.ucorealty.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repository = DB helper. No SQL needed for basic CRUD (like a typed fetch wrapper).
public interface ProjectRepository extends JpaRepository<Project, String> {
  List<Project> findByStatusAndIsActiveAndIsPublished(String status, Integer active, Integer published);
  List<Project> findByZoneNameIgnoreCase(String zoneName);
}
