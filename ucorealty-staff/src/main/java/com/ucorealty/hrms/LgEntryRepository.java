package com.ucorealty.hrms;

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

  /**
   * Leads of the REALTY channel, newest first. Every filter is optional.
   * SQL (built below):  select ... from LG_ENTRY_REVAMP where CHANNEL = 'REALTY'
   *                     [and date between from and to] [and STATUS = ?] [and name / mobile / lead id contains text]
   *                     order by date desc
   */
  public List<LeadRow> find(LocalDate from, LocalDate to, String status, String search) {
    StringBuilder where = new StringBuilder(" WHERE CHANNEL = ?");
    List<Object> args = new ArrayList<>();
    args.add(CHANNEL);
    if (from != null) {
      where.append(" AND ").append(LEAD_DATE).append(" >= ?");
      args.add(Timestamp.valueOf(from.atStartOfDay()));
    }
    if (to != null) {
      where.append(" AND ").append(LEAD_DATE).append(" < ?");           // up to the end of the "to" day
      args.add(Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }
    if (status != null && !status.isBlank()) {
      where.append(" AND STATUS = ?");
      args.add(status.trim());
    }
    if (search != null && !search.isBlank()) {
      String like = "%" + search.trim().toUpperCase() + "%";
      where.append(" AND (UPPER(REF_NAME) LIKE ? OR MOBILE_NO LIKE ? OR UPPER(LEAD_ID) LIKE ?)");
      args.add(like);
      args.add("%" + search.trim() + "%");
      args.add(like);
    }
    String sql = "SELECT LEAD_ID, REF_NAME, MOBILE_NO, EMAIL, STATE, DISTRICT, FACILITY_INT, LOANAMOUNT, TENT_LOAN_AMT, STATUS, "
        + LEAD_DATE + " AS LEAD_DT FROM %sLG_ENTRY_REVAMP" + where + " ORDER BY " + LEAD_DATE + " DESC";

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
        }, args.toArray());
      } catch (DataAccessException e) {
        log.warn("[UCO LEAD] Could not read {}LG_ENTRY_REVAMP: {}", prefix, e.getMostSpecificCause().getMessage());
      }
    }
    return new ArrayList<>();   // table not readable (permission / wrong schema): the page shows "no leads"; the log says why
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
