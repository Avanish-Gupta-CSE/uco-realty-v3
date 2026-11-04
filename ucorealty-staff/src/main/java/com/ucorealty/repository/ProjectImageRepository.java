package com.ucorealty.repository;

import com.ucorealty.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
  List<ProjectImage> findByProjectId(String projectId);

  /** Photos of a staging request (not live yet). */
  List<ProjectImage> findByStagingId(Long stagingId);
}
