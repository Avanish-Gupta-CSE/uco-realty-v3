package com.ucorealty.repository;

import com.ucorealty.entity.LeadTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeadTransferLogRepository extends JpaRepository<LeadTransferLog, Long> {
}
