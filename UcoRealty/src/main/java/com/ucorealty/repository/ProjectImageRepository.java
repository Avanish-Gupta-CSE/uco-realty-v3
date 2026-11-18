package com.ucorealty.repository;

import com.ucorealty.model.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Reads the photos of live projects (table realty_project_images; PDF files are in ProjectAttachmentRepository). save / findById come from JpaRepository.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * Queries that need only a few columns return a small interface (below) instead of the whole row, so the photo bytes
 * are not loaded just to build a list of pictures. The "AS" names in quotes must match the getter names.
 * Change this file when: the catalog or detail page needs other photo data.
 */
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

  /** The id of the first photo of a project (catalog thumbnail). */
  interface FirstPhoto {
    Long getImageId();
    String getProjectId();
  }

  /** The id of one photo. */
  interface PhotoId {
    Long getImageId();
  }

  /** For each of these projects, the id of its first photo (oldest id). */
  @Query(nativeQuery = true, value = """
      SELECT MIN(id) AS "imageId", project_id AS "projectId"
      FROM realty_project_images
      WHERE project_id IN (:projectIds)
      GROUP BY project_id
      """)
  List<FirstPhoto> findFirstImageIds(@Param("projectIds") Collection<String> projectIds);

  /** The ids of all photos of a project, oldest first (detail page gallery). */
  @Query(nativeQuery = true, value = """
      SELECT id AS "imageId"
      FROM realty_project_images
      WHERE project_id = :projectId
      ORDER BY id
      """)
  List<PhotoId> findPhotoIds(@Param("projectId") String projectId);
}
