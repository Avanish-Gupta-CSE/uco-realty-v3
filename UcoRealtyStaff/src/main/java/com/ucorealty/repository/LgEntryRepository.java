package com.ucorealty.repository;

import com.ucorealty.model.LeadRow;
import com.ucorealty.util.DatabaseTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the customer leads that the bank's Home Loan page saved in the HRMS table LG_ENTRY_REVAMP.
 * Our portal sends customers there with ?utm=REALTY, and the bank stores CHANNEL = 'REALTY' on those rows,
 * so "our" leads are simply the rows with that channel. READ ONLY: this application never writes to the table.
 * The table name (with its database user) is written once in util/DatabaseTables.
 *
 * Every query is written the same way: @Query(nativeQuery = true, value = "plain SQL"). The SQL is joined from short lines with +
 * because the table name comes from DatabaseTables. A filter that is not used is switched off with a 0/1 flag instead of
 * a null value (a null in a query value is unreliable on Oracle). The three public methods without @Query (find, count, statuses)
 * turn the page's filters (dates, status, search text) into those flags and values, so the controllers can pass them as they are.
 * Locally (h2 profile) the table comes from db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql + 05_LOCAL_TEST_EMPLOYEES.sql.
 * Change this file when: the Leads page needs another filter or another column (then also model/LeadRow).
 */
public interface LgEntryRepository extends Repository<LeadRow, String> {

  Logger log = LoggerFactory.getLogger(LgEntryRepository.class);

  /** Used for a date filter that is switched off (the value is ignored by the SQL, but must not be null). */
  Timestamp NO_DATE = Timestamp.valueOf("1900-01-01 00:00:00");

  /**
   * The conditions shared by the list and the count. The lead's date is SUBMIT_DATE (CRE_DT when that is empty).
   * :useFrom / :useTo / :useStatus / :useSearch are 1 when the page's filter is set and 0 when it is not.
   * "to" is the first moment AFTER the last day to include. The search matches part of the name, the mobile number or the lead id.
   */
  String LEAD_CONDITIONS =
      "WHERE CHANNEL = 'REALTY' "
    + "  AND (:useFrom = 0 OR NVL(SUBMIT_DATE, CRE_DT) >= :from) "
    + "  AND (:useTo = 0 OR NVL(SUBMIT_DATE, CRE_DT) < :to) "
    + "  AND (:useStatus = 0 OR STATUS = :status) "
    + "  AND (:useSearch = 0 OR UPPER(REF_NAME) LIKE '%' || UPPER(:search) || '%' "
    + "                      OR MOBILE_NO LIKE '%' || :search || '%' "
    + "                      OR UPPER(LEAD_ID) LIKE '%' || UPPER(:search) || '%') ";

  /** One page of matching leads, newest first (LEAD_ID breaks ties, so rows never repeat or vanish between pages). */
  @Query(nativeQuery = true, value =
      "SELECT LEAD_ID, REF_NAME, MOBILE_NO, EMAIL, STATE, DISTRICT, FACILITY_INT, LOANAMOUNT, TENT_LOAN_AMT, STATUS, SUBMIT_DATE, CRE_DT "
    + "FROM " + DatabaseTables.LEADS + " "
    + LEAD_CONDITIONS
    + "ORDER BY NVL(SUBMIT_DATE, CRE_DT) DESC, LEAD_ID DESC "
    + "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY")
  List<LeadRow> findLeads(@Param("useFrom") int useFrom, @Param("from") Timestamp from,
                          @Param("useTo") int useTo, @Param("to") Timestamp to,
                          @Param("useStatus") int useStatus, @Param("status") String status,
                          @Param("useSearch") int useSearch, @Param("search") String search,
                          @Param("offset") int offset, @Param("limit") int limit);

  /** How many leads match the same conditions (for the page numbers). */
  @Query(nativeQuery = true, value =
      "SELECT COUNT(*) FROM " + DatabaseTables.LEADS + " "
    + LEAD_CONDITIONS)
  long countLeads(@Param("useFrom") int useFrom, @Param("from") Timestamp from,
                  @Param("useTo") int useTo, @Param("to") Timestamp to,
                  @Param("useStatus") int useStatus, @Param("status") String status,
                  @Param("useSearch") int useSearch, @Param("search") String search);

  /** The distinct status codes present for the REALTY channel (fills the status filter on the Leads page). */
  @Query(nativeQuery = true, value =
      "SELECT DISTINCT STATUS FROM " + DatabaseTables.LEADS + " "
    + "WHERE CHANNEL = 'REALTY' AND STATUS IS NOT NULL "
    + "ORDER BY STATUS")
  List<String> findStatuses();

  // ---------------------------------------------------------------------------------------------------------------
  // The methods the controllers call. They only prepare the values for the queries above.
  // If the table cannot be read (no permission, wrong owner name), the Leads page shows "no leads" and the log says why.
  // ---------------------------------------------------------------------------------------------------------------

  /** All matching leads, newest first (used by the Excel export). Every filter is optional (null = not used). */
  default List<LeadRow> find(LocalDate from, LocalDate to, String status, String search) {
    return find(from, to, status, search, 0, 0);
  }

  /** One page of matching leads: skip `offset` rows, return at most `limit` (limit 0 = all of them). */
  default List<LeadRow> find(LocalDate from, LocalDate to, String status, String search, int offset, int limit) {
    try {
      return findLeads(
          from != null ? 1 : 0, from != null ? Timestamp.valueOf(from.atStartOfDay()) : NO_DATE,
          to != null ? 1 : 0, to != null ? Timestamp.valueOf(to.plusDays(1).atStartOfDay()) : NO_DATE,
          hasText(status) ? 1 : 0, hasText(status) ? status.trim() : "-",
          hasText(search) ? 1 : 0, hasText(search) ? search.trim() : "-",
          Math.max(0, offset), limit > 0 ? limit : Integer.MAX_VALUE);
    } catch (DataAccessException e) {
      log.warn("[UCO LEAD] Could not read {}: {}", DatabaseTables.LEADS, e.getMostSpecificCause().getMessage());
      return new ArrayList<>();
    }
  }

  /** How many leads match the same filters. */
  default int count(LocalDate from, LocalDate to, String status, String search) {
    try {
      return (int) countLeads(
          from != null ? 1 : 0, from != null ? Timestamp.valueOf(from.atStartOfDay()) : NO_DATE,
          to != null ? 1 : 0, to != null ? Timestamp.valueOf(to.plusDays(1).atStartOfDay()) : NO_DATE,
          hasText(status) ? 1 : 0, hasText(status) ? status.trim() : "-",
          hasText(search) ? 1 : 0, hasText(search) ? search.trim() : "-");
    } catch (DataAccessException e) {
      log.warn("[UCO LEAD] Could not count {}: {}", DatabaseTables.LEADS, e.getMostSpecificCause().getMessage());
      return 0;
    }
  }

  /** The status codes for the filter drop-down. */
  default List<String> statuses() {
    try {
      return findStatuses();
    } catch (DataAccessException e) {
      log.debug("[UCO LEAD] Status list of {} failed: {}", DatabaseTables.LEADS, e.getMessage());
      return new ArrayList<>();
    }
  }

  private static boolean hasText(String text) {
    return text != null && !text.isBlank();
  }
}
