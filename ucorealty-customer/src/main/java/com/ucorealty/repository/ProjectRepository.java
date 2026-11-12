package com.ucorealty.repository;

import com.ucorealty.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reads the live projects (table realty_projects). save / findById / delete come from JpaRepository and need no SQL.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * Change this file when: customers should see a different set of projects.
 */
public interface ProjectRepository extends JpaRepository<Project, String> {

  /** The live projects customers may see: status APPROVED, active, and published. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_projects
      WHERE status = :status
        AND is_active = :active
        AND is_published = :published
      """)
  List<Project> findByStatusAndIsActiveAndIsPublished(@Param("status") String status,
                                                      @Param("active") Integer active,
                                                      @Param("published") Integer published);
}
