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
 * The queries that need only a few columns return a small interface (FirstPhoto, FileInfo) instead of the whole row, so the photo
 * bytes are not loaded just to build a list of pictures. (The whole rows below are for saving, deleting and approving.) The "AS" names in quotes must match the getter names.
 * Change this file when: a screen needs other photo or document data.
 */
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

  /** The id of the first photo of a project (catalog thumbnail). */
  interface FirstPhoto {
    Long getImageId();
    String getProjectId();
  }

  /** One uploaded file WITHOUT its bytes: enough to show a list of pictures and links (the bytes are read by /files/{id} only). */
  interface FileInfo {
    Long getId();
    String getFileKind();
    String getFileName();
    default boolean isPhoto() { return getFileKind() == null || ProjectImage.PHOTO.equals(getFileKind()); }
  }

  /** Id and kind of every file (photos and documents) of a live project, oldest first. No bytes are read. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_kind AS "fileKind", file_name AS "fileName"
      FROM realty_project_images
      WHERE project_id = :projectId
      ORDER BY id
      """)
  List<FileInfo> findFilesOfProject(@Param("projectId") String projectId);

  /** Id and kind of every file of a staging request (work not live yet), oldest first. No bytes are read. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_kind AS "fileKind", file_name AS "fileName"
      FROM realty_project_images
      WHERE staging_id = :stagingId
      ORDER BY id
      """)
  List<FileInfo> findFilesOfStaging(@Param("stagingId") Long stagingId);

  /** How many photos a live project has. */
  @Query(nativeQuery = true, value = """
      SELECT COUNT(*) FROM realty_project_images
      WHERE project_id = :projectId
        AND file_kind = 'PHOTO'
      """)
  long countPhotos(@Param("projectId") String projectId);

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
