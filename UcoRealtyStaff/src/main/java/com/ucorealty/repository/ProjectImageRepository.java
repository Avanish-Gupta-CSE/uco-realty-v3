package com.ucorealty.repository;

import com.ucorealty.model.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Reads the photos and PDF documents (table realty_project_images). save / findById / deleteAll come from JpaRepository.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * The query that needs only two columns returns a small interface (FirstPhoto) instead of the whole row, so the photo bytes
 * are not loaded just to build a list of pictures. The "AS" names in quotes must match the getter names.
 * Change this file when: a screen needs other photo or document data.
 */
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

  /** The id of the first photo of a project (catalog thumbnail). */
  interface FirstPhoto {
    Long getImageId();
    String getProjectId();
  }

  /** Every file (photos and documents) of a live project, whole rows. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_images
      WHERE project_id = :projectId
      """)
  List<ProjectImage> findByProjectId(@Param("projectId") String projectId);

  /** Every file of a staging request (work of a maker that is not live yet), whole rows. */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_project_images
      WHERE staging_id = :stagingId
      """)
  List<ProjectImage> findByStagingId(@Param("stagingId") Long stagingId);

  /** For each of these projects, the id of its first photo (oldest id). */
  @Query(nativeQuery = true, value = """
      SELECT MIN(id) AS "imageId", project_id AS "projectId"
      FROM realty_project_images
      WHERE project_id IN (:projectIds)
        AND file_kind = 'PHOTO'
      GROUP BY project_id
      """)
  List<FirstPhoto> findFirstImageIds(@Param("projectIds") Collection<String> projectIds);
}
