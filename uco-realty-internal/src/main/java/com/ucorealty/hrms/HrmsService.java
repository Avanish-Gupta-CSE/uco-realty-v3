package com.ucorealty.hrms;

import com.ucorealty.entity.AppUser;
import java.util.List;

public interface HrmsService {

  HrmsUserProfile fetchUserProfile(String empId);

  boolean validateEmployee(String empId);

  String generateOtp(String empId);

  boolean verifyOtp(String empId, String otp);

  List<HrmsUserProfile> listAllDirectoryEmployees();

  AppUser syncAppUser(HrmsUserProfile profile);

  /** Portal role for this posting, or null when the employee has no access. */
  String resolveRole(HrmsUserProfile profile);

  /** Official zone name for zonal-office employees, otherwise null. */
  String resolveZone(HrmsUserProfile profile);
}

