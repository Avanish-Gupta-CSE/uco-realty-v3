package com.ucorealty.repository;

import com.ucorealty.model.HrmsEmployee;
import com.ucorealty.util.DatabaseTables;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Reads the bank's HRMS tables. READ ONLY: this application never writes to them (so this interface has no save or delete).
 * The table names (with their database user) are written once in util/DatabaseTables.
 * Every query below is written the same way: @Query(nativeQuery = true, value = "plain SQL"). Here the SQL is joined from
 * short lines with + because the table name comes from DatabaseTables (a text block cannot contain a constant).
 * Change this file when: the sign-in needs another fact about the employee.
 */
public interface HrmsRepository extends Repository<HrmsEmployee, String> {

  /** The current posting of an employee (the row valid today). It overrides SOL, scale and designation of the contact row. */
  interface CurrentPosting {
    String getPostingSol();
    String getScaleCode();
    String getTenureAs();
  }

  /** The number and name of an employee (to show names instead of numbers). */
  interface EmployeeName {
    String getEmpNo();
    String getName();
  }

  /** The employee with this number (who they are and where they were posted), or empty when HRMS does not know them. */
  @Query(nativeQuery = true, value =
      "SELECT EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, "
    + "       POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL, RETIRE_FLAG "
    + "FROM " + DatabaseTables.EMPLOYEE + " "
    + "WHERE EMPNO = :empNo")
  Optional<HrmsEmployee> findByEmpNo(@Param("empNo") String empNo);

  /** The posting row that is valid today (valid from/to dates and status V), if the employee has one. */
  @Query(nativeQuery = true, value =
      "SELECT POSTING_SOL AS \"postingSol\", SCALE_CODE AS \"scaleCode\", TENURE_AS AS \"tenureAs\" "
    + "FROM " + DatabaseTables.EMPLOYEE_TENURE + " "
    + "WHERE EMPNO = :empNo "
    + "  AND SYSDATE BETWEEN EFF_FROM_DATE AND EFF_TO_DATE "
    + "  AND STATUS_FLAG = 'V' "
    + "FETCH FIRST 1 ROWS ONLY")
  Optional<CurrentPosting> findCurrentPosting(@Param("empNo") String empNo);

  /** Number and name of several employees in one query. Unknown numbers are simply missing from the answer. */
  @Query(nativeQuery = true, value =
      "SELECT EMPNO AS \"empNo\", NAME AS \"name\" "
    + "FROM " + DatabaseTables.EMPLOYEE + " "
    + "WHERE EMPNO IN (:empNos)")
  List<EmployeeName> findNames(@Param("empNos") Collection<String> empNos);
}
