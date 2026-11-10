package com.ucorealty.repository;

import com.ucorealty.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
  // SQL: select * from realty_login_audit order by login_time desc   (one page at a time)
  Page<LoginAudit> findAllByOrderByLoginTimeDesc(Pageable pageable);
}

