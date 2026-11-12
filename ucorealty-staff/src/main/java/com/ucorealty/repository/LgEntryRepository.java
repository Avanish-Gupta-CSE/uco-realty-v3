package com.ucorealty.repository;

import com.ucorealty.dto.LeadRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the customer leads that the bank's Home Loan page saved in the HRMS table LG_ENTRY_REVAMP.
 * Our portal sends customers there with ?utm=REALTY, and the bank stores CHANNEL = 'REALTY' on those rows,
 * so "our" leads are simply the rows with that channel. READ ONLY: this application never writes to the table.
 *
 * Like HrmsRepository it tries the schema prefixes of uco.hrms.table-prefixes in order (no prefix, then hrms_dba.).
 * Locally (h2 profile) the table comes from db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql + 05_LOCAL_TEST_EMPLOYEES.sql.
 */
@Repository
public class LgEntryRepository {
  private static final Logger log = LoggerFactory.getLogger(LgEntryRepository.class);

  /** The value the bank writes in CHANNEL for leads that came from UCO Realty. */
  public static final String CHANNEL = "REALTY";

  // the lead's date is SUBMIT_DATE; CRE_DT is the fallback when the bank left it empty
  private static final String LEAD_DATE = "NVL(SUBMIT_DATE, CRE_DT)";

  private final JdbcTemplate jdbc;
  private final HrmsRepository hrms;

  public LgEntryRepository(JdbcTemplate jdbc, HrmsRepository hrms) {
    this.jdbc = jdbc;
    this.hrms = hrms;
  }

  /** The filter part of the SQL ("WHERE CHANNEL = ? AND ...") and its values, shared by the list and the count. */
  private static final class Filter {
    final StringBuilder where = new StringBuilder(" WHERE CHANNEL = ?");
    final List<Object> args = new ArrayList<>();
  }

  private static Filter filter(LocalDate from, LocalDate to, String status, String search) {
    Filter f = new Filter();
    f.args.add(CHANNEL);
    if (from != null) {
      f.where.append(" AND ").append(LEAD_DATE).append(" >= ?");
      f.args.add(Timestamp.valueOf(from.atStartOfDay()));
    }
    if (to != null) {
      f.where.append(" AND ").append(LEAD_DATE).append(" < ?");           // up to the end of the "to" day
      f.args.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }
    if (status != null && !status.isBlank()) {
      f.where.append(" AND STATUS = ?");
      f.args.add(status.trim());
    }
    if (search != null && !search.isBlank()) {
      String like = "%" + search.trim().toUpperCase() + "%";
      f.where.append(" AND (UPPER(REF_NAME) LIKE ? OR MOBILE_NO LIKE ? OR UPPER(LEAD_ID) LIKE ?)");
      f.args.add(like);
      f.args.add("%" + search.trim() + "%");
      f.args.add(like);
    }
    return f;
  }

  /** All matching leads, newest first (used by the Excel export). Every filter is optional. */
  public List<LeadRow> find(LocalDate from, LocalDate to, String status, String search) {
    return find(from, to, status, search, 0, 0);
  }

  /**
   * One page of matching leads, newest first: the database skips `offset` rows and returns at most `limit` (limit 0 = no limit).
   * SQL (built below):  select ... from LG_ENTRY_REVAMP where CHANNEL = 'REALTY'
   *                     [and date between from and to] [and STATUS = ?] [and name / mobile / lead id contains text]
   *                     order by date desc, lead id desc [offset ? rows fetch next ? rows only]
   */
  public List<LeadRow> find(LocalDate from, LocalDate to, String status, String search, int offset, int limit) {
    Filter f = filter(from, to, status, search);
    String sql = "SELECT LEAD_ID, REF_NAME, MOBILE_NO, EMAIL, STATE, DISTRICT, FACILITY_INT, LOANAMOUNT, TENT_LOAN_AMT, STATUS, "
        + LEAD_DATE + " AS LEAD_DT FROM %sLG_ENTRY_REVAMP" + f.where + " ORDER BY " + LEAD_DATE + " DESC, LEAD_ID DESC";   // LEAD_ID breaks ties, so rows never repeat or vanish between pages
    if (limit > 0) {
      sql += " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
      f.args.add(Math.max(0, offset));
      f.args.add(limit);
    }
    for (String prefix : hrms.tablePrefixes()) {
      try {
        return jdbc.query(String.format(sql, prefix), (rs, i) -> {
          LeadRow r = new LeadRow();
          r.leadId = rs.getString("LEAD_ID");
          r.name = rs.getString("REF_NAME");
          r.mobile = rs.getString("MOBILE_NO");
          r.email = rs.getString("EMAIL");
          r.state = rs.getString("STATE");
          r.district = rs.getString("DISTRICT");
          r.facility = rs.getString("FACILITY_INT");
          r.loanAmountText = rs.getString("LOANAMOUNT");
          r.tentativeLoanAmount = rs.getBigDecimal("TENT_LOAN_AMT");
          r.status = rs.getString("STATUS");
          r.submittedAt = rs.getTimestamp("LEAD_DT");
          return r;
        }, f.args.toArray());
      } catch (DataAccessException e) {
        log.warn("[UCO LEAD] Could not read {}LG_ENTRY_REVAMP: {}", prefix, e.getMostSpecificCause().getMessage());
      }
    }
    return new ArrayList<>();   // table not readable (permission / wrong schema): the page shows "no leads"; the log says why
  }

  /** How many leads match the same filters (for the page numbers). */
  public int count(LocalDate from, LocalDate to, String status, String search) {
    Filter f = filter(from, to, status, search);
    for (String prefix : hrms.tablePrefixes()) {
      try {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + prefix + "LG_ENTRY_REVAMP" + f.where, Integer.class, f.args.toArray());
        return n == null ? 0 : n;
      } catch (DataAccessException e) {
        log.warn("[UCO LEAD] Could not count {}LG_ENTRY_REVAMP: {}", prefix, e.getMostSpecificCause().getMessage());
      }
    }
    return 0;
  }

  /** The distinct lead status codes present for the REALTY channel (fills the status filter on the Leads page). */
  public List<String> statuses() {
    for (String prefix : hrms.tablePrefixes()) {
      try {
        return jdbc.queryForList("SELECT DISTINCT STATUS FROM " + prefix + "LG_ENTRY_REVAMP WHERE CHANNEL = ? AND STATUS IS NOT NULL ORDER BY 1",
            String.class, CHANNEL);
      } catch (DataAccessException e) {
        log.debug("[UCO LEAD] Status list on '{}LG_ENTRY_REVAMP' failed: {}", prefix, e.getMessage());
      }
    }
    return new ArrayList<>();
  }
}
