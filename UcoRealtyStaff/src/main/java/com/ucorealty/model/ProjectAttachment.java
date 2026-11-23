package com.ucorealty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One PDF attachment of a project (table realty_project_attachments): a brochure, a floor plan, an approval letter or any other PDF,
 * stored in the database as a BLOB. A project can have as many as the maker adds, each at most 3 MB.
 * It belongs either to a live project (projectId) or to a maker's unapproved work (stagingId), never to both.
 * Photos are a different table (ProjectImage).
 * Change this file when: an attachment needs another property.
 */
@Entity
@Table(name = "realty_project_attachments")
public class ProjectAttachment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // set for a live (approved) attachment; null while it belongs to a maker's unapproved work (stagingId)
  @Column(name = "project_id", length = 20)
  public String projectId;

  // set while the attachment is part of a staging request: never shown to customers
  @Column(name = "staging_id")
  public Long stagingId;

  @Column(name = "file_name", length = 255, nullable = false)
  public String fileName;

  // BROCHURE, FLOOR_PLAN, RERA or OTHER (see util/DocTypes); the title is typed by the maker for an OTHER document
  @Column(name = "doc_type", length = 20, nullable = false)
  public String docType = com.ucorealty.util.DocTypes.OTHER;

  @Column(name = "doc_title", length = 100)
  public String docTitle;

  @Column(name = "mime_type", length = 100)
  public String mimeType;

  @Column(name = "file_size")
  public Long fileSize;

  // Stored directly in Oracle as a SecureFiles BLOB
  @Lob
  @Basic(fetch = FetchType.LAZY)
  @JsonIgnore
  @Column(name = "file_data")
  public byte[] fileData;

  @Column(name = "uploaded_by", length = 50)
  public String uploadedBy;

  @Column(name = "uploaded_at")
  public LocalDateTime uploadedAt;

  @PrePersist
  void onCreate() {
    uploadedAt = LocalDateTime.now();
  }

  // --- Getters (the pages read the values through these; the database layer fills the fields directly, so no setters) ---
  public Long getId() { return id; }
  public String getProjectId() { return projectId; }
  public String getFileName() { return fileName; }
  public String getDocType() { return docType; }
  public String getDocTitle() { return docTitle; }
  public String getMimeType() { return mimeType; }
  public Long getFileSize() { return fileSize; }
  public byte[] getFileData() { return fileData; }
  public String getUploadedBy() { return uploadedBy; }
  public LocalDateTime getUploadedAt() { return uploadedAt; }
}
