package com.ucorealty.hrms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The only place that reads the bank's HRMS tables (read only: this application never writes to them).
 *
 * Tables: employee_contact_details (who the employee is and where posted), employee_tenure (current posting, overrides the
 * contact row) and employee_job_profile (optional). They live in the same database as our own tables, either in our schema
 * (or reached through a synonym) or in the HRMS schema "hrms_dba". Which of the two is a bank question, so the prefixes
 * to try, in order, are a setting: uco.hrms.table-prefixes (env HRMS_TABLE_PREFIXES), default ",hrms_dba." = first no prefix, then hrms_dba.
 *
 * In the h2 and uat profiles these tables are created from db_migrations/03_CREATE_HRMS_MOCK_TABLES.sql and filled by 05_LOCAL_TEST_EMPLOYEES.sql,
 * so the code path is identical everywhere; there are no test employees in the Java code.
 */
@Repository
public class HrmsRepository {

  private static final Logger logger = LoggerFactory.getLogger(HrmsRepository.class);
  private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Za-z0-9_]*\\.?");

  private static final String CONTACT_SQL =
      "SELECT EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL FROM %semployee_contact_details WHERE EMPNO = ?";
  private static final String TENURE_SQL =
      "SELECT POSTING_SOL, SCALE_CODE, TENURE_AS FROM %semployee_tenure WHERE EMPNO = ? AND SYSDATE BETWEEN EFF_FROM_DATE AND EFF_TO_DATE AND STATUS_FLAG = 'V'";
  private static final String JOB_PROFILE_SQL =
      "SELECT JOB_PROFILE FROM %semployee_job_profile WHERE EMPNO = ? AND SYSDATE BETWEEN EFF_FROM_DATE AND EFF_TO_DATE AND STATUS_FLAG = 'V'";

  private final JdbcTemplate jdbc;
  private final List<String> prefixes = new ArrayList<>();

  public HrmsRepository(JdbcTemplate jdbc, @Value("${uco.hrms.table-prefixes:,hrms_dba.}") String tablePrefixes) {
    this.jdbc = jdbc;
    for (String prefix : tablePrefixes.split(",", -1)) {
      String p = prefix.trim();
      if (!SAFE_PREFIX.matcher(p).matches()) {
        throw new IllegalStateException("uco.hrms.table-prefixes: '" + p + "' is not a schema prefix such as hrms_dba.");
      }
      prefixes.add(p);
    }
  }

  /** The employee with this number, or empty when HRMS does not know them (or the tables cannot be read). */
  public Optional<HrmsUserProfile> findByEmpNo(String empNo) {
    for (String prefix : prefixes) {
      try {
        List<HrmsUserProfile> rows = jdbc.query(String.format(CONTACT_SQL, prefix), (rs, i) -> {
          HrmsUserProfile p = new HrmsUserProfile();
          p.setEmpNo(rs.getString("EMPNO"));
          p.setName(rs.getString("NAME"));
          p.setPostingSolId(rs.getString("POSTING_SOL_ID"));
          p.setPostingSolDesc(rs.getString("POSTING_SOL_DESC"));
          p.setScaleCode(rs.getString("SCALE_CODE"));
          p.setCadre(rs.getString("CADRE_CODE"));
          p.setPostingRegionName(rs.getString("POSTING_REGION_NAME"));
          p.setTenureAs(rs.getString("DESIG"));
          p.setMobile(rs.getString("MOBILE_NO"));
          p.setEmail(rs.getString("E_MAIL"));
          p.setActive(true);
          return p;
        }, empNo);
        if (!rows.isEmpty()) {
          HrmsUserProfile p = rows.get(0);
          addTenure(p);
          addJobProfile(p);
          return Optional.of(p);
        }
      } catch (DataAccessException e) {
        logger.warn("[UCO HRMS] Could not read {}employee_contact_details: {}", prefix, e.getMostSpecificCause().getMessage());
      }
    }
    return Optional.empty();
  }

  /** The current posting row overrides SOL, scale and designation of the contact row. A missing table is not an error. */
  private void addTenure(HrmsUserProfile p) {
    for (String prefix : prefixes) {
      try {
        jdbc.query(String.format(TENURE_SQL, prefix), (ResultSetExtractor<Void>) rs -> {
          if (rs.next()) {
            String sol = rs.getString("POSTING_SOL");
            String scale = rs.getString("SCALE_CODE");
            String tenure = rs.getString("TENURE_AS");
            if (sol != null && !sol.isEmpty()) p.setPostingSolId(sol);
            if (scale != null && !scale.isEmpty()) p.setScaleCode(scale);
            if (tenure != null && !tenure.isEmpty()) p.setTenureAs(tenure);
          }
          return null;
        }, p.getEmpNo());
        return;   // this prefix has the table: done, even when the employee has no current row
      } catch (DataAccessException e) {
        logger.debug("[UCO HRMS] Tenure lookup on '{}employee_tenure' failed: {}", prefix, e.getMessage());
      }
    }
  }

  private void addJobProfile(HrmsUserProfile p) {
    for (String prefix : prefixes) {
      try {
        jdbc.query(String.format(JOB_PROFILE_SQL, prefix), (ResultSetExtractor<Void>) rs -> {
          if (rs.next()) p.setJobProfile(rs.getString("JOB_PROFILE"));
          return null;
        }, p.getEmpNo());
        return;
      } catch (DataAccessException e) {
        logger.debug("[UCO HRMS] Job-profile lookup on '{}employee_job_profile' failed: {}", prefix, e.getMessage());
      }
    }
  }
}
