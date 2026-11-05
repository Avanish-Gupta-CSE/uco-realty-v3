package com.ucorealty.hrms;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UAT DEMO USERS: the only place in the application with fixed logins (employee number + password).
 *
 * Used only when uco.auth.directory=demo. The "uat" profile sets that, the "prod" profile refuses to start with it.
 * Their roles are not stored here: each user has a posting (SOL, scale, cadre) like a real HRMS record, and RoleResolver
 * turns it into a role exactly as it does for real employees (7115 = DIT superadmin, 8028 = HO Marketing,
 * 70xx scale 1-3 = zonal maker, 70xx scale 4+ / EXEC = zonal checker).
 *
 * To change a user or a password: edit the list below and rebuild the WAR.
 * Before production: set uco.auth.directory=hrms (or delete this class and its few references): employees then come only
 * from the bank HRMS tables and the bank SSO.
 */
@Component
public class DemoUserDirectory {

  public static class DemoUser {
    private final String empNo, password, roleLabel, name, solId, solDesc, scale, cadre, region;

    DemoUser(String empNo, String password, String roleLabel, String name, String solId, String solDesc, String scale, String cadre, String region) {
      this.empNo = empNo; this.password = password; this.roleLabel = roleLabel; this.name = name;
      this.solId = solId; this.solDesc = solDesc; this.scale = scale; this.cadre = cadre; this.region = region;
    }
    public String getEmpNo() { return empNo; }
    public String getRoleLabel() { return roleLabel; }     // shown on the login page (the password is never exposed)
  }

  //                                 empNo     password           shown as                  name                   SOL    posting                                      scale  cadre   region
  private static final List<DemoUser> USERS = List.of(
      new DemoUser("UAT001", "Dit@Uat2026",   "DIT Superadmin",         "UAT DIT Superadmin",  "7115", "DIT-SWDC HEAD OFFICE",                             "3", "OFFR", "HEAD OFFICE"),
      new DemoUser("UAT002", "Mkt@Uat2026",   "HO Marketing",           "UAT HO Marketing",    "8028", "HO MARKETING AND WEALTH MANAGEMENT DEPARTMENT",    "4", "EXEC", "HEAD OFFICE"),
      new DemoUser("UAT003", "Maker@Uat2026", "Zonal Maker (Ajmer)",    "UAT Zonal Maker",     "7015", "AJMER ZONAL OFFICE",                               "2", "OFFR", "AJMER"),
      new DemoUser("UAT004", "Check@Uat2026", "Zonal Checker (Ajmer)",  "UAT Zonal Checker",   "7015", "AJMER ZONAL OFFICE",                               "4", "EXEC", "AJMER"));

  private static final int MAX_FAILURES = 5;
  private static final long LOCK_MILLIS = 5 * 60 * 1000L;
  private final Map<String, long[]> failures = new ConcurrentHashMap<>();   // empNo -> {count, time of last failure}

  public List<DemoUser> users() { return USERS; }

  /** The user as an HRMS-style profile (so the normal role rules apply), or null when there is no such demo user. */
  public HrmsUserProfile find(String empNo) {
    DemoUser u = lookup(empNo);
    if (u == null) return null;
    HrmsUserProfile p = new HrmsUserProfile();
    p.setEmpNo(u.empNo);
    p.setName(u.name);
    p.setPostingSolId(u.solId);
    p.setPostingSolDesc(u.solDesc);
    p.setScaleCode(u.scale);
    p.setCadre(u.cadre);
    p.setPostingRegionName(u.region);
    p.setTenureAs(u.roleLabel);
    return p;
  }

  /** True when empNo and password match a demo user. After 5 wrong passwords that user is locked for 5 minutes. */
  public boolean passwordMatches(String empNo, String password) {
    DemoUser u = lookup(empNo);
    if (u == null || password == null) return false;
    long[] f = failures.get(u.empNo);
    if (f != null && f[0] >= MAX_FAILURES) {
      if (System.currentTimeMillis() - f[1] < LOCK_MILLIS) return false;
      failures.remove(u.empNo);
    }
    boolean ok = MessageDigest.isEqual(u.password.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
    if (ok) {
      failures.remove(u.empNo);
    } else {
      failures.merge(u.empNo, new long[] {1, System.currentTimeMillis()}, (old, one) -> new long[] {old[0] + 1, one[1]});
    }
    return ok;
  }

  private DemoUser lookup(String empNo) {
    if (empNo == null) return null;
    String wanted = empNo.trim().toUpperCase(Locale.ROOT);
    for (DemoUser u : USERS) {
      if (u.empNo.equals(wanted)) return u;
    }
    return null;
  }
}
