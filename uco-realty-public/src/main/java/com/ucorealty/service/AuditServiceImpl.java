package com.ucorealty.service;

import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.LoginAudit;
import com.ucorealty.repository.AuditLogRepository;
import com.ucorealty.repository.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {
  private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

  private final AuditLogRepository auditLogRepository;
  private final LoginAuditRepository loginAuditRepository;

  public AuditServiceImpl(AuditLogRepository auditLogRepository, LoginAuditRepository loginAuditRepository) {
    this.auditLogRepository = auditLogRepository;
    this.loginAuditRepository = loginAuditRepository;
  }

  @Override
  public void logActivity(String username, String action, String entityType, String entityId, String details) {
    logActivity(username, action, entityType, entityId, null, details);
  }

  @Override
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

  @Override
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

  @Override
  @Transactional(readOnly = true)
  public Page<AuditLog> getActivityLogs(Pageable pageable) {
    return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LoginAudit> getLoginAudits(Pageable pageable) {
    return loginAuditRepository.findAllByOrderByLoginTimeDesc(pageable);
  }
}

