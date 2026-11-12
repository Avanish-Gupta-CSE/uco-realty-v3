package com.ucorealty.repository;

import com.ucorealty.model.ProjectStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
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

  /** Oracle refuses an IN list with more than 1000 values (ORA-01795), so long lists are asked in groups of this size. */
  int MAX_IDS_PER_QUERY = 500;

  /**
   * Same as findByProjectIdInAndStatusIn, but safe for any number of projects: it asks the database in groups of
   * MAX_IDS_PER_QUERY project codes and joins the answers. Call this one from the services.
   */
  default List<ProjectStaging> findForProjects(List<String> projectIds, Collection<String> statuses) {
    List<ProjectStaging> found = new ArrayList<>();
    for (int from = 0; from < projectIds.size(); from += MAX_IDS_PER_QUERY) {
      int to = Math.min(from + MAX_IDS_PER_QUERY, projectIds.size());
      found.addAll(findByProjectIdInAndStatusIn(projectIds.subList(from, to), statuses));
    }
    return found;
  }

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
