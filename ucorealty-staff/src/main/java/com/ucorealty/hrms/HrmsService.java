package com.ucorealty.hrms;

import com.ucorealty.entity.AppUser;

import java.util.List;

public interface HrmsService {

  /** The employee as HRMS knows them, or null when unknown. */
  HrmsUserProfile fetchUserProfile(String empId);

  /** True when the fixed test OTP (uco.sso.test-otp) is configured; never in production. */
  boolean isTestOtpEnabled();

  /** Checks the test OTP for an employee who exists in HRMS. The real OTP is the bank SSO portal's job. */
  boolean verifyOtp(String empId, String otp);

  /** UAT demo: true when employees come from the four fixed demo users (uco.auth.directory=demo) instead of the HRMS tables. */
  boolean isDemoDirectory();

  /** The demo users to list on the login page; empty unless the demo directory is on. */
  List<DemoUserDirectory.DemoUser> demoUsers();

  /** Demo login: employee number + password of a demo user. Always false unless the demo directory is on. */
  boolean verifyDemoPassword(String empNo, String password);

  AppUser toAppUser(HrmsUserProfile profile);

  /** Portal role for this posting, or null when the employee has no access. */
  String resolveRole(HrmsUserProfile profile);

  /** Official zone name for zonal-office employees, otherwise null. */
  String resolveZone(HrmsUserProfile profile);
}
