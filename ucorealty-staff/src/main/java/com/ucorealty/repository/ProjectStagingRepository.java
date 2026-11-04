package com.ucorealty.repository;

import com.ucorealty.entity.ProjectStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectStagingRepository extends JpaRepository<ProjectStaging, Long> {
  /** The one open request (DRAFT, PENDING_APPROVAL or NEEDS_CORRECTION) of a project, if any. */
  Optional<ProjectStaging> findFirstByProjectIdAndStatusIn(String projectId, Collection<String> statuses);

  List<ProjectStaging> findByProjectIdInAndStatusIn(Collection<String> projectIds, Collection<String> statuses);

  List<ProjectStaging> findByRequestTypeAndStatusIn(String requestType, Collection<String> statuses);

  List<ProjectStaging> findByProjectIdOrderByIdDesc(String projectId);
}
