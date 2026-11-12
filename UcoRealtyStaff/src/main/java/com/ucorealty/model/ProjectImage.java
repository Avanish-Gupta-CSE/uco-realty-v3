package com.ucorealty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One uploaded file (table realty_project_images): a photo, or a PDF (brochure / floor plan), stored in the database as a BLOB.
 * It belongs either to a live project (projectId) or to a maker's unapproved work (stagingId), never to both.
 * Change this file when: a file needs another property.
 */
@Entity
@Table(name = "realty_project_images")
public class ProjectImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // set for a live (approved) photo; null while the photo belongs to a maker's unapproved work (stagingId)
  @Column(name = "project_id", length = 20)
  public String projectId;

  // set while the photo is part of a staging request: never shown to customers
  @Column(name = "staging_id")
  public Long stagingId;

  // PHOTO (gallery), BROCHURE or FLOOR_PLAN (PDF documents): one table holds all uploaded files
  public static final String PHOTO = "PHOTO";
  public static final String BROCHURE = "BROCHURE";
  public static final String FLOOR_PLAN = "FLOOR_PLAN";

  @Column(name = "file_kind", length = 20, nullable = false)
  public String fileKind = PHOTO;

  public boolean isPhoto() { return fileKind == null || PHOTO.equals(fileKind); }
  public String getFileKind() { return fileKind; }

  @Column(name = "file_name", length = 255)
  public String fileName;

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

  // Informational only (root-relative, no context path). Never send this to a browser: build URLs from the image id
  // with request.getContextPath() + "/files/" + id, because on Tomcat the app lives under /<war name>.
  @Column(name = "file_path", length = 500)
  public String filePath;

  @Column(name = "is_verified")
  public Integer isVerified = 0;

  @Column(name = "uploaded_by", length = 50)
  public String uploadedBy;

  @Column(name = "uploaded_at")
  public LocalDateTime uploadedAt;

  @PrePersist
  void onCreate() {
    uploadedAt = LocalDateTime.now();
    if (filePath == null) {
      filePath = "/files/pending";
    }
  }

  // --- Getters (the pages read the values through these; the database layer fills the fields directly, so no setters) ---
  public Long getId() { return id; }
  public String getProjectId() { return projectId; }
  public String getFileName() { return fileName; }
  public String getMimeType() { return mimeType; }
  public Long getFileSize() { return fileSize; }
  public byte[] getFileData() { return fileData; }
  public String getFilePath() { return filePath; }
  public Integer getIsVerified() { return isVerified; }
  public String getUploadedBy() { return uploadedBy; }
  public LocalDateTime getUploadedAt() { return uploadedAt; }
}
