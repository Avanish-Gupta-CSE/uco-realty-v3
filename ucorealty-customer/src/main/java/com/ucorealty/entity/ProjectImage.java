package com.ucorealty.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "realty_project_images")
public class ProjectImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  // null while the photo belongs to a maker's unapproved work (never shown to customers)
  @Column(name = "project_id", length = 20)
  public String projectId;

  // PHOTO (gallery), BROCHURE or FLOOR_PLAN (PDF documents): one table holds all uploaded files
  public static final String PHOTO = "PHOTO";
  public static final String BROCHURE = "BROCHURE";
  public static final String FLOOR_PLAN = "FLOOR_PLAN";

  @Column(name = "file_kind", length = 20, nullable = false)
  public String fileKind = PHOTO;

  public boolean isPhoto() { return fileKind == null || PHOTO.equals(fileKind); }
  public String getFileKind() { return fileKind; }
  public void setFileKind(String fileKind) { this.fileKind = fileKind; }

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

  // --- JavaBean Getters & Setters for JSP EL support ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getProjectId() { return projectId; }
  public void setProjectId(String projectId) { this.projectId = projectId; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  public Long getFileSize() { return fileSize; }
  public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
  public byte[] getFileData() { return fileData; }
  public void setFileData(byte[] fileData) { this.fileData = fileData; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Integer getIsVerified() { return isVerified; }
  public void setIsVerified(Integer isVerified) { this.isVerified = isVerified; }
  public String getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
  public LocalDateTime getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
