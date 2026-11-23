package com.ucorealty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * One PDF attachment of a live project (table realty_project_attachments): a brochure, a floor plan or any other PDF. Read only here.
 * Photos are a different table (ProjectImage).
 * Change this file when: the staff app's model/ProjectAttachment changes (the two classes describe the same table).
 */
@Entity
@Table(name = "realty_project_attachments")
public class ProjectAttachment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // null while the attachment belongs to a maker's unapproved work (never shown to customers)
  @Column(name = "project_id", length = 20)
  public String projectId;

  @Column(name = "file_name", length = 255, nullable = false)
  public String fileName;

  @Column(name = "doc_type", length = 20)
  public String docType;

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

  public Long getId() { return id; }
  public String getProjectId() { return projectId; }
  public String getFileName() { return fileName; }
  public String getDocType() { return docType; }
  public String getDocTitle() { return docTitle; }
  public String getMimeType() { return mimeType; }
  public Long getFileSize() { return fileSize; }
  public byte[] getFileData() { return fileData; }
}
