package com.ucorealty.service;

import com.ucorealty.model.AuditLog;
import com.ucorealty.model.LoginAudit;
import com.ucorealty.repository.AuditLogRepository;
import com.ucorealty.repository.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the two audit trails: what staff DID (realty_audit_log) and who SIGNED IN (realty_login_audit).
 * A failure to write an audit row is logged but never stops the user's action.
 * Change this file when: a new kind of audit entry is needed.
 */
@Service
@Transactional
public class AuditService {
  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogRepository auditLogRepository;
  private final LoginAuditRepository loginAuditRepository;

  public AuditService(AuditLogRepository auditLogRepository, LoginAuditRepository loginAuditRepository) {
    this.auditLogRepository = auditLogRepository;
    this.loginAuditRepository = loginAuditRepository;
  }

  /** Records an action with one detail text (no old value). */
  public void logActivity(String username, String action, String entityType, String entityId, String details) {
    logActivity(username, action, entityType, entityId, null, details);
  }

  /** Records an action with the value before and after. Missing parts are stored as SYSTEM / N/A / UNKNOWN. */
  public void logActivity(String username, String action, String entityType, String entityId, String oldVal, String newVal) {
    try {
      AuditLog audit = new AuditLog(
          entityType != null ? entityType : "SYSTEM",
          entityId != null ? entityId : "N/A",
          action != null ? action : "UNKNOWN",
          username != null ? username : "SYSTEM",
          oldVal,
          newVal
      );
      auditLogRepository.save(audit);
    } catch (Exception e) {
      log.error("Failed to persist activity audit log: {}", e.getMessage(), e);
    }
  }

  /** Records one sign-in attempt (successful or refused) with the client address. */
  public void logLoginAttempt(String username, String ipAddress, boolean success, String remarks) {
    try {
      LoginAudit audit = new LoginAudit(
          username != null ? username : "UNKNOWN",
          ipAddress != null ? ipAddress : "UNKNOWN",
          success,
          remarks
      );
      loginAuditRepository.save(audit);
    } catch (Exception e) {
      log.error("Failed to persist login audit log: {}", e.getMessage(), e);
    }
  }

  /** One page of the activity trail, newest first (Logs page). */
  @Transactional(readOnly = true)
  public Page<AuditLog> getActivityLogs(Pageable pageable) {
    return new PageImpl<>(auditLogRepository.findNewestFirst(pageable.getOffset(), pageable.getPageSize()), pageable, auditLogRepository.countAll());
  }

  /** One page of the sign-in trail, newest first (Logs page). */
  @Transactional(readOnly = true)
  public Page<LoginAudit> getLoginAudits(Pageable pageable) {
    return new PageImpl<>(loginAuditRepository.findNewestFirst(pageable.getOffset(), pageable.getPageSize()), pageable, loginAuditRepository.countAll());
  }

  /** The approval steps of one project (created, submitted, approved, rejected, ...), oldest first. */
  @Transactional(readOnly = true)
  public java.util.List<AuditLog> projectSteps(String projectId, java.util.Collection<String> actions) {
    return auditLogRepository.findProjectSteps(projectId, actions);
  }
}
