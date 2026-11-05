package com.ucorealty.hrms;

import com.ucorealty.entity.AppUser;

public interface HrmsService {

  /** The employee as HRMS knows them, or null when unknown. */
  HrmsUserProfile fetchUserProfile(String empId);

  /** True when the fixed test OTP (uco.sso.test-otp) is configured; never in production. */
  boolean isTestOtpEnabled();

  /** Checks the test OTP for an employee who exists in HRMS. The real OTP is the bank SSO portal's job. */
  boolean verifyOtp(String empId, String otp);

  AppUser toAppUser(HrmsUserProfile profile);

  /** Portal role for this posting, or null when the employee has no access. */
  String resolveRole(HrmsUserProfile profile);

  /** Official zone name for zonal-office employees, otherwise null. */
  String resolveZone(HrmsUserProfile profile);
}
