package com.ucorealty.repository;

import com.ucorealty.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reads the approved projects (table realty_projects). save / findById / existsById come from JpaRepository and need no SQL.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * Change this file when: a screen needs a different list of approved projects.
 */
public interface ProjectRepository extends JpaRepository<Project, String> {

  /** The live projects: status APPROVED, active, and published (what customers see). */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_projects
      WHERE status = :status
        AND is_active = :active
        AND is_published = :published
      """)
  List<Project> findByStatusAndIsActiveAndIsPublished(@Param("status") String status,
                                                      @Param("active") Integer active,
                                                      @Param("published") Integer published);

  /** Every project of one zone, upper or lower case does not matter (what a zonal maker or checker may see). */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_projects
      WHERE UPPER(zone_name) = UPPER(:zoneName)
      """)
  List<Project> findByZoneNameIgnoreCase(@Param("zoneName") String zoneName);
}
