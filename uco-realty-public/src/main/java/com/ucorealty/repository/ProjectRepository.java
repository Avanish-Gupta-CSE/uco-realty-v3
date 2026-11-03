package com.ucorealty.repository;

import com.ucorealty.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, String> {
  List<Project> findByStatusAndIsActiveAndIsPublished(String status, Integer active, Integer published);
}
