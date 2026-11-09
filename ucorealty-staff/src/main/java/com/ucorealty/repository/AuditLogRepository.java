package com.ucorealty.repository;

import com.ucorealty.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  // SQL: select * from audit_log order by created_at desc   (one page at a time)
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
