package com.ucorealty.repository;

import com.ucorealty.model.ProjectAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reads the PDF attachments of live projects (table realty_project_attachments). findById comes from JpaRepository.
 * The list query returns a small interface (FileInfo) instead of the whole row, so the PDF bytes are not loaded just to
 * build a list of links. The "AS" names in quotes must match the getter names.
 * Change this file when: the detail page needs other attachment data.
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

  /** The attachments of a live project, oldest first. */
  @Query(nativeQuery = true, value = """
      SELECT id AS "id", file_name AS "fileName", doc_type AS "docType", doc_title AS "docTitle"
      FROM realty_project_attachments
      WHERE project_id = :projectId
      ORDER BY id
      """)
  List<FileInfo> findFilesOfProject(@Param("projectId") String projectId);
}
