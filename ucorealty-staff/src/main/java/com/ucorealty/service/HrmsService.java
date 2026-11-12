package com.ucorealty.service;

import com.ucorealty.model.HrmsUserProfile;
import com.ucorealty.model.AppUser;
import com.ucorealty.repository.HrmsRepository;
import com.ucorealty.util.DatabaseTables;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Who is this employee, and what may they do here. The employee data comes from the bank HRMS tables (HrmsRepository),
 * the role from RoleResolver. The OTP step belongs to the bank SSO portal; the only OTP this class knows is the optional
 * fixed test OTP (uco.sso.test-otp) for closed UAT/laptop use.
 * Change this file when: the sign-in checks change. (Roles by SOL: RoleResolver. Table names: HrmsRepository.)
 */
@Service
public class HrmsService {

  private static final Logger logger = LoggerFactory.getLogger(HrmsService.class);

  // UAT bring-up only (until the bank SSO is wired): a fixed OTP for real HRMS employees. Empty = off (the default).
  // Refused at startup in the prod profile; every use is logged as a warning.
  @Value("${uco.sso.test-otp:}")
  private String testOtp;

  private final HrmsRepository hrmsRepository;
  private final RoleResolver roleResolver;
  private final Environment environment;

  public HrmsService(HrmsRepository hrmsRepository, RoleResolver roleResolver, Environment environment) {
    this.hrmsRepository = hrmsRepository;
    this.roleResolver = roleResolver;
    this.environment = environment;
  }

  @PostConstruct
  public void init() {
    if (isTestOtpEnabled()) {
      if (environment.acceptsProfiles(Profiles.of("prod"))) {
        throw new IllegalStateException("uco.sso.test-otp must never be set in the prod profile");
      }
      logger.warn("[UCO HRMS] *** TEST OTP IS ENABLED *** Any employee found in HRMS can sign in with the fixed test OTP. Use only on a closed UAT or a laptop.");
    }
  }

  /**
   * The employee as HRMS knows them, or null when unknown, retired, or when the table cannot be read (the log says why).
   * Two steps: the contact row says who the employee is; the current posting row (if any) overrides SOL, scale and designation.
   */
  public HrmsUserProfile fetchUserProfile(String empId) {
    if (empId == null || empId.trim().isEmpty()) {
      return null;
    }
    HrmsUserProfile profile;
    try {
      profile = hrmsRepository.findByEmpNo(empId.trim()).orElse(null);
    } catch (DataAccessException e) {
      logger.warn("[UCO HRMS] Could not read {}: {}", DatabaseTables.EMPLOYEE, e.getMostSpecificCause().getMessage());
      return null;
    }
    if (profile == null) {
      return null;
    }
    if (!profile.isActive()) {
      logger.warn("[UCO ACCESS] Employee {} is retired (RETIRE_FLAG=RT): access denied", profile.getEmpNo());
      return null;
    }
    applyCurrentPosting(profile);
    return profile;
  }

  /** The current posting row overrides SOL, scale and designation of the contact row. A missing posting is not an error. */
  private void applyCurrentPosting(HrmsUserProfile profile) {
    try {
      Optional<HrmsRepository.CurrentPosting> posting = hrmsRepository.findCurrentPosting(profile.getEmpNo());
      if (posting.isPresent()) {
        if (hasText(posting.get().getPostingSol())) profile.setPostingSolId(posting.get().getPostingSol());
        if (hasText(posting.get().getScaleCode())) profile.setScaleCode(posting.get().getScaleCode());
        if (hasText(posting.get().getTenureAs())) profile.setTenureAs(posting.get().getTenureAs());
      }
    } catch (DataAccessException e) {
      logger.debug("[UCO HRMS] Posting lookup on {} failed: {}", DatabaseTables.EMPLOYEE_TENURE, e.getMessage());
    }
  }

  /** empNo -> name for these employees (one query), so screens can show names instead of numbers. */
  public Map<String, String> namesOf(Collection<String> empNos) {
    Map<String, String> names = new HashMap<>();
    List<String> wanted = new ArrayList<>();
    for (String empNo : empNos) {
      if (empNo != null && !empNo.isBlank()) wanted.add(empNo);
    }
    if (wanted.isEmpty()) {
      return names;
    }
    try {
      for (HrmsRepository.EmployeeName row : hrmsRepository.findNames(wanted)) {
        names.put(row.getEmpNo(), row.getName());
      }
    } catch (DataAccessException e) {
      logger.debug("[UCO HRMS] Name lookup on {} failed: {}", DatabaseTables.EMPLOYEE, e.getMessage());
    }
    return names;
  }

  private static boolean hasText(String text) {
    return text != null && !text.isEmpty();
  }

  /** True when the fixed test OTP (uco.sso.test-otp) is configured; never in production. */
  public boolean isTestOtpEnabled() {
    return testOtp != null && !testOtp.trim().isEmpty();
  }

  /** Checks the test OTP for an employee who exists in HRMS. The real OTP is the bank SSO portal's job. */
  public boolean verifyOtp(String empId, String otp) {
    if (!isTestOtpEnabled() || otp == null || fetchUserProfile(empId) == null) {
      return false;
    }
    boolean ok = testOtp.trim().equals(otp.trim());
    if (ok) logger.warn("[UCO HRMS] Employee {} signed in with the TEST OTP", empId.trim());
    return ok;
  }

  /** Portal role for this posting, or null when the employee has no access. */
  public String resolveRole(HrmsUserProfile profile) {
    return roleResolver.resolveRole(profile);
  }

  /** Official zone name for zonal-office employees, otherwise null. */
  public String resolveZone(HrmsUserProfile profile) {
    return roleResolver.resolveZone(profile);
  }

  /** The signed-in user object (employee number, role, zone, office name) built from an HRMS profile. */
  public AppUser toAppUser(HrmsUserProfile profile) {
    if (profile == null) return null;
    return new AppUser(profile.getEmpNo(), resolveRole(profile), resolveZone(profile), profile.getPostingSolDesc());
  }
}
