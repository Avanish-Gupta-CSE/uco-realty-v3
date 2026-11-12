package com.ucorealty.repository;

import com.ucorealty.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reads the activity trail (table realty_audit_log). save comes from JpaRepository.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL").
 * Change this file when: the Logs page needs other data.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /** One page of the trail, newest first: skip `offset` rows, return at most `limit` (id breaks ties, so no row repeats between pages). */
  @Query(nativeQuery = true, value = """
      SELECT * FROM realty_audit_log
      ORDER BY created_at DESC, id DESC
      OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
      """)
  List<AuditLog> findNewestFirst(@Param("offset") long offset, @Param("limit") int limit);

  /** How many rows the trail has (for the page numbers). */
  @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM realty_audit_log")
  long countAll();
}
