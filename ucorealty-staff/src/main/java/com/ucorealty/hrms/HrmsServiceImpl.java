package com.ucorealty.hrms;

import com.ucorealty.entity.AppUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Who is this employee, and what may they do here. The employee data comes from the bank HRMS tables (HrmsRepository),
 * the role from RoleResolver. The OTP step belongs to the bank SSO portal; the only OTP this class knows is the optional
 * fixed test OTP (uco.sso.test-otp) for closed UAT/laptop use.
 */
@Service
public class HrmsServiceImpl implements HrmsService {

  private static final Logger logger = LoggerFactory.getLogger(HrmsServiceImpl.class);

  // UAT bring-up only (until the bank SSO is wired): a fixed OTP for real HRMS employees. Empty = off (the default).
  // Refused at startup in the prod profile; every use is logged as a warning.
  @Value("${uco.sso.test-otp:}")
  private String testOtp;

  // hrms = the bank HRMS tables (the only value allowed in prod); demo = the four fixed UAT users of DemoUserDirectory
  @Value("${uco.auth.directory:hrms}")
  private String directory;

  private final HrmsRepository hrmsRepository;
  private final DemoUserDirectory demoDirectory;
  private final RoleResolver roleResolver;
  private final Environment environment;

  public HrmsServiceImpl(HrmsRepository hrmsRepository, DemoUserDirectory demoDirectory, RoleResolver roleResolver, Environment environment) {
    this.hrmsRepository = hrmsRepository;
    this.demoDirectory = demoDirectory;
    this.roleResolver = roleResolver;
    this.environment = environment;
  }

  @PostConstruct
  public void init() {
    if (!"hrms".equals(directory) && !"demo".equals(directory)) {
      throw new IllegalStateException("uco.auth.directory must be 'hrms' or 'demo', not '" + directory + "'");
    }
    if (isDemoDirectory()) {
      if (environment.acceptsProfiles(Profiles.of("prod"))) {
        throw new IllegalStateException("uco.auth.directory=demo (UAT demo users) must never be used in the prod profile");
      }
      logger.warn("[UCO HRMS] *** UAT DEMO USERS ARE ENABLED *** Employees are the fixed users of DemoUserDirectory, not the bank HRMS. Use only on a closed UAT.");
    }
    if (isTestOtpEnabled()) {
      if (environment.acceptsProfiles(Profiles.of("prod"))) {
        throw new IllegalStateException("uco.sso.test-otp must never be set in the prod profile");
      }
      logger.warn("[UCO HRMS] *** TEST OTP IS ENABLED *** Any employee found in HRMS can sign in with the fixed test OTP. Use only on a closed UAT or a laptop.");
    }
  }

  @Override
  public HrmsUserProfile fetchUserProfile(String empId) {
    if (empId == null || empId.trim().isEmpty()) {
      return null;
    }
    if (isDemoDirectory()) {
      return demoDirectory.find(empId);
    }
    return hrmsRepository.findByEmpNo(empId.trim()).orElse(null);
  }

  @Override
  public boolean isDemoDirectory() {
    return "demo".equals(directory);
  }

  @Override
  public List<DemoUserDirectory.DemoUser> demoUsers() {
    return isDemoDirectory() ? demoDirectory.users() : Collections.emptyList();
  }

  @Override
  public boolean verifyDemoPassword(String empNo, String password) {
    boolean ok = isDemoDirectory() && demoDirectory.passwordMatches(empNo, password);
    if (ok) logger.warn("[UCO HRMS] Demo user {} signed in with a UAT demo password", empNo.trim());
    return ok;
  }

  @Override
  public boolean isTestOtpEnabled() {
    return testOtp != null && !testOtp.trim().isEmpty();
  }

  @Override
  public boolean verifyOtp(String empId, String otp) {
    if (!isTestOtpEnabled() || otp == null || fetchUserProfile(empId) == null) {
      return false;
    }
    boolean ok = testOtp.trim().equals(otp.trim());
    if (ok) logger.warn("[UCO HRMS] Employee {} signed in with the TEST OTP", empId.trim());
    return ok;
  }

  @Override
  public String resolveRole(HrmsUserProfile profile) {
    return roleResolver.resolveRole(profile);
  }

  @Override
  public String resolveZone(HrmsUserProfile profile) {
    return roleResolver.resolveZone(profile);
  }

  @Override
  public AppUser toAppUser(HrmsUserProfile profile) {
    if (profile == null) return null;
    return new AppUser(profile.getEmpNo(), resolveRole(profile), resolveZone(profile), profile.getPostingSolDesc());
  }
}
