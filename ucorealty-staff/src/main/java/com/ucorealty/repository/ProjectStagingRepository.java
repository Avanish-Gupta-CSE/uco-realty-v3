package com.ucorealty.repository;

import com.ucorealty.model.ProjectStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Reads the makers' working copies and requests (table realty_project_staging). save / findById come from JpaRepository.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * Change this file when: the workflow screens need a different list of requests.
 */
public interface ProjectStagingRepository extends JpaRepository<ProjectStaging, Long> {

  /** The one open request (DRAFT, PENDING_APPROVAL or NEEDS_CORRECTION) of a project, if any. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_staging
      WHERE project_id = :projectId
        AND status IN (:statuses)
      ORDER BY id
      FETCH FIRST 1 ROWS ONLY
      """)
  Optional<ProjectStaging> findFirstByProjectIdAndStatusIn(@Param("projectId") String projectId,
                                                           @Param("statuses") Collection<String> statuses);

  /** The requests with one of these statuses for any of these projects. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_staging
      WHERE project_id IN (:projectIds)
        AND status IN (:statuses)
      """)
  List<ProjectStaging> findByProjectIdInAndStatusIn(@Param("projectIds") Collection<String> projectIds,
                                                    @Param("statuses") Collection<String> statuses);

  /** The requests of one type (for example CREATE) with one of these statuses. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_staging
      WHERE request_type = :requestType
        AND status IN (:statuses)
      """)
  List<ProjectStaging> findByRequestTypeAndStatusIn(@Param("requestType") String requestType,
                                                    @Param("statuses") Collection<String> statuses);

  /** The whole history of one project, newest request first. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_staging
      WHERE project_id = :projectId
      ORDER BY id DESC
      """)
  List<ProjectStaging> findByProjectIdOrderByIdDesc(@Param("projectId") String projectId);
}
