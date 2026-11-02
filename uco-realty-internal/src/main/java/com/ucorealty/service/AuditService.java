package com.ucorealty.service;

import com.ucorealty.entity.AuditLog;
import com.ucorealty.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {
  void logActivity(String username, String action, String entityType, String entityId, String details);
  void logActivity(String username, String action, String entityType, String entityId, String oldVal, String newVal);
  void logLoginAttempt(String username, String ipAddress, boolean success, String remarks);
  Page<AuditLog> getActivityLogs(Pageable pageable);
  Page<LoginAudit> getLoginAudits(Pageable pageable);
}

