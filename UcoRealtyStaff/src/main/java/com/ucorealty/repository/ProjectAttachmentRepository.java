package com.ucorealty.repository;

import com.ucorealty.model.ProjectAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Reads and changes the PDF attachments (table realty_project_attachments). save / findById / deleteById come from JpaRepository.
 * Every query below is written the same way: @Query(nativeQuery = true, value = """ plain SQL """).
 * The list queries return a small interface (FileInfo) instead of the whole row, and the approve / delete queries work on the
 * table directly, so the PDF bytes (up to 3 MB each, as many as a project has) are never loaded just to count, list or move files.
 * The "AS" names in quotes must match the getter names.
 * Change this file when: a screen needs other attachment data.
 */
public interface ProjectAttachmentRepository extends JpaRepository<ProjectAttachment, Long> {

  /** One attachment WITHOUT its bytes: enough to show a list of links (the bytes are read by /attachments/{id} only). */
  interface FileInfo {
    Long getId();
    String getFileName();
    String getDocType();
    String getDocTitle();

    /** The text people see for this file: the slot label (Brochure, ...), the maker's title, or the file name. */
    default String getDisplayName() { return com.ucorealty.util.DocTypes.display(getDocType(), getDocTitle(), getFileName()); }
  }

  /** The attachments of a live project, oldest first. No bytes are read. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_name AS "fileName", doc_type AS "docType", doc_title AS "docTitle"
      FROM realty_project_attachments
      WHERE project_id = :projectId
      ORDER BY id
      """)
  List<FileInfo> findFilesOfProject(@Param("projectId") String projectId);

  /** The attachments of a staging request (work not live yet), oldest first. No bytes are read. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_name AS "fileName", doc_type AS "docType", doc_title AS "docTitle"
      FROM realty_project_attachments
      WHERE staging_id = :stagingId
      ORDER BY id
      """)
  List<FileInfo> findFilesOfStaging(@Param("stagingId") Long stagingId);

  /** These attachments by id (to show the ones a change request removes). No bytes are read. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_name AS "fileName", doc_type AS "docType", doc_title AS "docTitle"
      FROM realty_project_attachments
      WHERE id IN (:ids)
      ORDER BY id
      """)
  List<FileInfo> findFilesByIds(@Param("ids") Collection<Long> ids);

  /** Approval: every attachment of the request becomes an attachment of the live project. Returns how many moved. */
  @Modifying(flushAutomatically = true)
  @Query(nativeQuery = true, value = """
      UPDATE realty_project_attachments
      SET project_id = :projectId, staging_id = NULL
      WHERE staging_id = :stagingId
      """)
  int makeLive(@Param("stagingId") Long stagingId, @Param("projectId") String projectId);

  /** The request is withdrawn or rejected: its attachments are deleted, they will never go live. */
  @Modifying(flushAutomatically = true)
  @Query(nativeQuery = true, value = """
      DELETE FROM realty_project_attachments
      WHERE staging_id = :stagingId
      """)
  int deleteOfStaging(@Param("stagingId") Long stagingId);

  /** Approval of a change: the live attachments the maker removed are deleted (only those of this project). */
  @Modifying(flushAutomatically = true)
  @Query(nativeQuery = true, value = """
      DELETE FROM realty_project_attachments
      WHERE project_id = :projectId
        AND id IN (:ids)
      """)
  int deleteLiveByIds(@Param("projectId") String projectId, @Param("ids") Collection<Long> ids);
}
