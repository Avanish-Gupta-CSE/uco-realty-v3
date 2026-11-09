package com.ucorealty.repository;

import com.ucorealty.entity.ProjectStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectStagingRepository extends JpaRepository<ProjectStaging, Long> {
  /** The one open request (DRAFT, PENDING_APPROVAL or NEEDS_CORRECTION) of a project, if any. */
  // SQL: select * from project_staging where project_id = ? and status in (...), first row only
  Optional<ProjectStaging> findFirstByProjectIdAndStatusIn(String projectId, Collection<String> statuses);

  // SQL: select * from project_staging where project_id in (...) and status in (...)
  List<ProjectStaging> findByProjectIdInAndStatusIn(Collection<String> projectIds, Collection<String> statuses);

  // SQL: select * from project_staging where request_type = ? and status in (...)
  List<ProjectStaging> findByRequestTypeAndStatusIn(String requestType, Collection<String> statuses);

  // SQL: select * from project_staging where project_id = ? order by id desc   (history of one project)
  List<ProjectStaging> findByProjectIdOrderByIdDesc(String projectId);
}
