package com.ucorealty.hrms;

import com.ucorealty.entity.AppUser;
import com.ucorealty.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HrmsServiceImpl implements HrmsService {

  private static final Logger logger = LoggerFactory.getLogger(HrmsServiceImpl.class);

  @Value("${uco.hrms.mode:bank}")
  private String hrmsMode;

  private final UserRepository userRepo;

  @Autowired(required = false)
  private DataSource dataSource;

  private final Map<String, HrmsUserProfile> mockProfiles = new LinkedHashMap<>();
  private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

  private static class OtpEntry {
    final String code;
    final Instant expiresAt;
    OtpEntry(String code, Instant expiresAt) {
      this.code = code;
      this.expiresAt = expiresAt;
    }
  }

  public HrmsServiceImpl(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @PostConstruct
  public void init() {
    if (isBankMode()) {
      logger.info("[UCO HRMS] Initialized in 'bank' mode: employees are read from the HRMS tables only.");
    } else {
      initMockProfiles();
      logger.warn("[UCO HRMS] Initialized in 'local' TEST mode with {} built-in employees. Never use this mode in UAT/production.",
          mockProfiles.size());
    }
  }

  private boolean isBankMode() {
    return "bank".equalsIgnoreCase(hrmsMode);
  }

  private void initMockProfiles() {
    // 1. Head Office DIT - Superadmin (SOL 7114)
    HrmsUserProfile dit = new HrmsUserProfile("100001", "Avanish Gupta", "7114", "DIT Head Office Kolkata",
        "Scale 4", "EXEC", "Head Office", "IT Executive");
    dit.setJobProfile("IT");
    dit.setMobile("9876543210");
    dit.setEmail("avanish.gupta@uco.bank.in");
    addMock(dit);

    // 2. Zonal Office Ajmer - Maker (SOL 7015, Scale 2 Officer)
    HrmsUserProfile ajmerMaker = new HrmsUserProfile("200001", "Rajesh Sharma", "7015", "Zonal Office Ajmer",
        "Scale 2", "OFFICER", "Ajmer", "Credit Officer");
    ajmerMaker.setJobProfile("Retail Credit");
    ajmerMaker.setMobile("9812345678");
    ajmerMaker.setEmail("rajesh.sharma@uco.bank.in");
    addMock(ajmerMaker);

    // 3. Zonal Office Ajmer - Checker (SOL 7015, Scale 4 Executive)
    HrmsUserProfile ajmerChecker = new HrmsUserProfile("200002", "Vikram Singh Rathore", "7015", "Zonal Office Ajmer",
        "Scale 4", "EXEC", "Ajmer", "Chief Manager / Zonal Approver");
    ajmerChecker.setJobProfile("Zonal Approver");
    ajmerChecker.setMobile("9812345679");
    ajmerChecker.setEmail("vikram.singh@uco.bank.in");
    addMock(ajmerChecker);

    // 4. Zonal Office Bhopal - Maker (SOL 7022, Scale 1 Officer)
    HrmsUserProfile bhopalMaker = new HrmsUserProfile("300001", "Pooja Verma", "7022", "Zonal Office Bhopal",
        "Scale 1", "OFFICER", "Bhopal", "Assistant Manager");
    bhopalMaker.setJobProfile("Housing Loans");
    bhopalMaker.setMobile("9823456780");
    bhopalMaker.setEmail("pooja.verma@uco.bank.in");
    addMock(bhopalMaker);

    // 5. Zonal Office Bhopal - Checker (SOL 7022, Scale 5 Executive)
    HrmsUserProfile bhopalChecker = new HrmsUserProfile("300002", "Anand Mishra", "7022", "Zonal Office Bhopal",
        "Scale 5", "EXEC", "Bhopal", "Deputy General Manager");
    bhopalChecker.setJobProfile("Zonal Head");
    bhopalChecker.setMobile("9823456781");
    bhopalChecker.setEmail("anand.mishra@uco.bank.in");
    addMock(bhopalChecker);

    // 6. Head Office Retail Banking (SOL 7105)
    HrmsUserProfile hoRetail = new HrmsUserProfile("400001", "Sanjay Mukherjee", "7105", "HO Retail Loan Hub",
        "Scale 3", "OFFICER", "Head Office", "Senior Manager Retail");
    hoRetail.setJobProfile("RETAIL");
    hoRetail.setMobile("9834567890");
    hoRetail.setEmail("sanjay.mukherjee@uco.bank.in");
    addMock(hoRetail);

    // 7. Head Office Marketing (SOL 7102)
    HrmsUserProfile hoMarketing = new HrmsUserProfile("400002", "Sunita Sen", "7102", "HO Marketing & PR",
        "Scale 4", "EXEC", "Head Office", "Chief Manager PR");
    hoMarketing.setJobProfile("MARKETING");
    hoMarketing.setMobile("9834567891");
    hoMarketing.setEmail("sunita.sen@uco.bank.in");
    addMock(hoMarketing);

    // 8. Head Office Admin (SOL 7101)
    HrmsUserProfile hoAdmin = new HrmsUserProfile("400003", "Debashis Roy", "7101", "HO General Administration",
        "Scale 4", "EXEC", "Head Office", "Chief Manager Administration");
    hoAdmin.setJobProfile("ADMIN");
    hoAdmin.setMobile("9834567892");
    hoAdmin.setEmail("debashis.roy@uco.bank.in");
    addMock(hoAdmin);

    // 9. Zonal Office Jaipur - Maker (SOL 7030, Scale 2 Officer)
    HrmsUserProfile jaipurMaker = new HrmsUserProfile("500001", "Neha Patel", "7030", "Zonal Office Jaipur",
        "Scale 2", "OFFICER", "Jaipur", "Manager Credit");
    jaipurMaker.setJobProfile("Retail Credit");
    jaipurMaker.setMobile("9845678901");
    jaipurMaker.setEmail("neha.patel@uco.bank.in");
    addMock(jaipurMaker);

    // 10. Zonal Office Jaipur - Checker (SOL 7030, Scale 4 Executive)
    HrmsUserProfile jaipurChecker = new HrmsUserProfile("500002", "Mahendra Choudhary", "7030", "Zonal Office Jaipur",
        "Scale 4", "EXEC", "Jaipur", "Chief Manager Credit");
    jaipurChecker.setJobProfile("Zonal Approver");
    jaipurChecker.setMobile("9845678902");
    jaipurChecker.setEmail("m.choudhary@uco.bank.in");
    addMock(jaipurChecker);

    // 11. Bank Production Staff (SOL 7115, Scale 3, SWDC - from bank image IMG_1026/1032)
    HrmsUserProfile swdcStaff = new HrmsUserProfile("61801", "Bank SWDC Developer", "7115", "SWDC Head Office",
        "Scale 3", "OFFICER", "Head Office", "Software Development Cell");
    swdcStaff.setJobProfile("SOFTWARE DEVELOPMENT CELL");
    swdcStaff.setMobile("9811122233");
    swdcStaff.setEmail("staff.61801@uco.bank.in");
    addMock(swdcStaff);
  }

  private void addMock(HrmsUserProfile profile) {
    mockProfiles.put(profile.getEmpNo(), profile);
  }

  @Override
  public HrmsUserProfile fetchUserProfile(String empId) {
    if (empId == null || empId.trim().isEmpty()) {
      return null;
    }
    String normalized = empId.trim();

    if (isBankMode()) {
      // Bank mode never falls back to built-in test employees.
      return fetchFromBankDatabase(normalized);
    }
    return mockProfiles.get(normalized);
  }

  @Override
  public boolean validateEmployee(String empId) {
    HrmsUserProfile p = fetchUserProfile(empId);
    return p != null && p.isActive();
  }

  @Override
  public String generateOtp(String empId) {
    HrmsUserProfile p = fetchUserProfile(empId);
    if (p == null) {
      return null;
    }

    if (isBankMode()) {
      return null; // The bank SSO portal performs the OTP step; this application never issues OTPs in bank mode.
    }
    String otp = "123456";

    otpStore.put(p.getEmpNo(), new OtpEntry(otp, Instant.now().plusSeconds(300)));

    logger.info("===============================================================");
    logger.info("[UCO BANK HRMS SSO OTP]");
    logger.info("  Employee ID : {}", p.getEmpNo());
    logger.info("  Name        : {}", p.getName());
    logger.info("  SOL ID      : {} ({})", p.getPostingSolId(), p.getPostingSolDesc());
    logger.info("  Cadre/Scale : {} / {}", p.getCadre(), p.getScaleCode());
    logger.info("  Role Mapped : {}", p.getResolvedRole());
    logger.info("  OTP Code    : {} [Valid for 5 minutes]", otp);
    logger.info("===============================================================");

    return otp;
  }

  @Override
  public boolean verifyOtp(String empId, String otp) {
    if (empId == null || otp == null) {
      return false;
    }
    HrmsUserProfile p = fetchUserProfile(empId);
    if (p == null) {
      return false;
    }

    if (isBankMode()) {
      return false;
    }
    if ("123456".equals(otp.trim())) {
      return true;
    }

    OtpEntry entry = otpStore.get(p.getEmpNo());
    if (entry == null) {
      return false;
    }

    if (Instant.now().isAfter(entry.expiresAt)) {
      otpStore.remove(p.getEmpNo());
      return false;
    }

    boolean matches = entry.code.equals(otp.trim());
    if (matches) {
      otpStore.remove(p.getEmpNo());
    }
    return matches;
  }

  @Override
  public List<HrmsUserProfile> listAllDirectoryEmployees() {
    return isBankMode() ? new ArrayList<>() : new ArrayList<>(mockProfiles.values());
  }

  @Override
  public AppUser syncAppUser(HrmsUserProfile profile) {
    if (profile == null) return null;

    String resolvedRole = profile.getResolvedRole();
    String resolvedZone = profile.getResolvedZone();
    String branch = profile.getPostingSolDesc();

    AppUser user = userRepo.findByUsername(profile.getEmpNo()).orElseGet(() -> {
      AppUser u = new AppUser();
      u.username = profile.getEmpNo();
      u.isActive = 1;
      return u;
    });

    user.role = resolvedRole;
    user.zone = resolvedZone;
    user.branch = branch;
    user.isActive = 1;
    AppUser saved = userRepo.save(user);

    return saved;
  }

  /**
   * Queries Bank's employee_contact_details table using the application's single primary DataSource.
   * Reference columns from bank screenshot IMG_1035.jpg & IMG_1026.jpg.
   */
  private HrmsUserProfile fetchFromBankDatabase(String empId) {
    if (dataSource == null) {
      logger.warn("[UCO HRMS] No primary DataSource available for Bank query.");
      return null;
    }

    // Try employee_contact_details (standard synonym or table in bank DB)
    String[] queries = {
      "SELECT EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL " +
      "FROM employee_contact_details WHERE EMPNO = ?",
      "SELECT EMPNO, NAME, POSTING_SOL_ID, POSTING_SOL_DESC, SCALE_CODE, CADRE_CODE, POSTING_REGION_NAME, DESIG, MOBILE_NO, E_MAIL " +
      "FROM hrms_dba.employee_contact_details WHERE EMPNO = ?"
    };

    for (String sql : queries) {
      try (Connection conn = dataSource.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, empId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
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

            // Enrich with tenure/job profile if available (IMG_1026/IMG_1033)
            enrichTenureAndJobProfile(conn, p);
            return p;
          }
        }
      } catch (SQLException e) {
        logger.warn("[UCO HRMS] HRMS query failed ({}): {}", e.getErrorCode(), e.getMessage());
      }
    }

    return null;
  }

  private static final String[] TABLE_PREFIXES = {"", "hrms_dba."};

  private void enrichTenureAndJobProfile(Connection conn, HrmsUserProfile p) {
    for (String prefix : TABLE_PREFIXES) {
      String tenureSql = "SELECT POSTING_SOL, SCALE_CODE, TENURE_AS FROM " + prefix + "employee_tenure " +
                         "WHERE EMPNO = ? AND SYSDATE BETWEEN EFF_FROM_DATE AND EFF_TO_DATE AND STATUS_FLAG = 'V'";
      try (PreparedStatement ps = conn.prepareStatement(tenureSql)) {
        ps.setString(1, p.getEmpNo());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            String sol = rs.getString("POSTING_SOL");
            String scale = rs.getString("SCALE_CODE");
            String tenure = rs.getString("TENURE_AS");
            if (sol != null && !sol.isEmpty()) p.setPostingSolId(sol);
            if (scale != null && !scale.isEmpty()) p.setScaleCode(scale);
            if (tenure != null && !tenure.isEmpty()) p.setTenureAs(tenure);
          }
          break;
        }
      } catch (SQLException e) {
        logger.debug("[UCO HRMS] Tenure lookup on '{}employee_tenure' failed: {}", prefix, e.getMessage());
      }
    }

    for (String prefix : TABLE_PREFIXES) {
      String jobSql = "SELECT JOB_PROFILE FROM " + prefix + "employee_job_profile " +
                      "WHERE EMPNO = ? AND SYSDATE BETWEEN EFF_FROM_DATE AND EFF_TO_DATE AND STATUS_FLAG = 'V'";
      try (PreparedStatement ps = conn.prepareStatement(jobSql)) {
        ps.setString(1, p.getEmpNo());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            p.setJobProfile(rs.getString("JOB_PROFILE"));
          }
          break;
        }
      } catch (SQLException e) {
        logger.debug("[UCO HRMS] Job-profile lookup on '{}employee_job_profile' failed: {}", prefix, e.getMessage());
      }
    }
  }
}
