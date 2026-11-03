package com.ucorealty.hrms;

import com.ucorealty.service.MasterDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns an HRMS posting into a portal role and zone. Nothing is assigned by hand: the role follows the
 * employee's posting SOL (and scale/cadre for zonal offices). Anything not listed gets no access.
 *
 * Configuration (application.yml, override per environment):
 *   uco.rbac.superadmin-sols  SOLs of DIT staff who may open the Logs page        (default 7115)
 *   uco.rbac.ho-admin-sols    HO departments with view-only access to projects     (default 8028)
 *   uco.rbac.marketing-sols   HO Marketing: create/approve projects in any zone    (default 8001)
 *   uco.rbac.retail-sols      HO Retail: customer leads, reduce unsold units       (no default, must be set)
 *   uco.rbac.zonal-sol-regex  SOLs that are zonal offices                          (default 70\d\d, e.g. 7001 Patna)
 * Zonal offices: Scale 4+ or EXEC cadre = Checker, Scale 1-3 = Maker; scale 0/blank (clerical) = no access.
 * The zone is taken from the SOL description ("PATNA ZONAL OFFICE" -> "Patna") and matched to the 49 official zones.
 */
@Component
public class RoleResolver {
  private static final Logger logger = LoggerFactory.getLogger(RoleResolver.class);

  private final Set<String> superadminSols;
  private final Set<String> hoAdminSols;
  private final Set<String> marketingSols;
  private final Set<String> retailSols;
  private final Pattern zonalSol;
  private final MasterDataService masterData;

  public RoleResolver(@Value("${uco.rbac.superadmin-sols:7115}") String superadmin,
                      @Value("${uco.rbac.ho-admin-sols:8028}") String hoAdmin,
                      @Value("${uco.rbac.marketing-sols:8001}") String marketing,
                      @Value("${uco.rbac.retail-sols:}") String retail,
                      @Value("${uco.rbac.zonal-sol-regex:70\\d\\d}") String zonalRegex,
                      MasterDataService masterData) {
    this.superadminSols = csv(superadmin);
    this.hoAdminSols = csv(hoAdmin);
    this.marketingSols = csv(marketing);
    this.retailSols = csv(retail);
    this.zonalSol = Pattern.compile(zonalRegex);
    this.masterData = masterData;
    logger.info("[UCO RBAC] superadmin SOLs={}, HO admin SOLs={}, marketing SOLs={}, retail SOLs={}, zonal SOL pattern={}",
        superadminSols, hoAdminSols, marketingSols, retailSols.isEmpty() ? "NOT SET" : retailSols, zonalRegex);
    if (retailSols.isEmpty()) {
      logger.warn("[UCO RBAC] uco.rbac.retail-sols is empty: nobody can sign in as HO Retail until the Retail Banking SOL is configured (UCO_RBAC_RETAIL_SOLS)");
    }
  }

  public String resolveRole(HrmsUserProfile p) {
    if (p == null) return null;
    String sol = digits(p.getPostingSolId());
    if (sol.isEmpty()) return null;

    if (superadminSols.contains(sol)) return "SUPERADMIN_DIT";
    if (marketingSols.contains(sol)) return "HO_MARKETING";
    if (retailSols.contains(sol)) return "HO_RETAIL";
    if (hoAdminSols.contains(sol)) return "HO_ADMIN";

    if (zonalSol.matcher(sol).matches()) {
      if (resolveZone(p) == null) {
        logger.warn("[UCO RBAC] Zonal SOL {} ('{}') does not match any official zone: no access", sol, p.getPostingSolDesc());
        return null;
      }
      boolean exec = "EXEC".equalsIgnoreCase(p.getCadre()) || "EXECUTIVE".equalsIgnoreCase(p.getCadre()) || "EX".equalsIgnoreCase(p.getCadre());
      int scale = scaleNumber(p.getScaleCode());
      if (exec || scale >= 4) return "ZO_CHECKER";
      if (scale >= 1) return "ZO_MAKER";
      return null;
    }
    return null;
  }

  /** Official zone name (e.g. "Patna") for a zonal-office employee, otherwise null. */
  public String resolveZone(HrmsUserProfile p) {
    if (p == null || !zonalSol.matcher(digits(p.getPostingSolId())).matches()) return null;
    String fromDesc = matchZone(p.getPostingSolDesc());
    return fromDesc != null ? fromDesc : matchZone(p.getPostingRegionName());
  }

  private String matchZone(String text) {
    String n = normalise(text);
    if (n.isEmpty()) return null;
    for (String zone : masterData.getZones()) {
      if (n.equals(normalise(zone))) return zone;
    }
    for (String zone : masterData.getZones()) {   // e.g. "PATNA REGION" style text
      if ((" " + n + " ").contains(" " + normalise(zone) + " ")) return zone;
    }
    return null;
  }

  /** Upper case, drops "ZONAL OFFICE", "ZONE", "ZO", brackets and punctuation. */
  private static String normalise(String raw) {
    if (raw == null) return "";
    String s = raw.toUpperCase(Locale.ROOT).replaceAll("\\(.*?\\)", " ");
    s = s.replaceAll("[^A-Z ]", " ");
    s = s.replaceAll("\\bZONAL\\b|\\bOFFICE\\b|\\bZONE\\b|\\bZO\\b", " ");
    return s.trim().replaceAll("\\s+", " ");
  }

  private static int scaleNumber(String code) {
    if (code == null || code.trim().isEmpty()) return 0;
    String d = digits(code);
    if (!d.isEmpty()) {
      try { return Integer.parseInt(d); } catch (NumberFormatException ignored) { return 0; }
    }
    String u = code.toUpperCase(Locale.ROOT).trim();
    if (u.contains("VII")) return 7;
    if (u.contains("VI")) return 6;
    if (u.contains("IV")) return 4;
    if (u.contains("V")) return 5;
    if (u.contains("III")) return 3;
    if (u.contains("II")) return 2;
    if (u.contains("I")) return 1;
    return 0;
  }

  private static String digits(String s) {
    return s == null ? "" : s.replaceAll("[^0-9]", "");
  }

  private static Set<String> csv(String list) {
    if (list == null || list.trim().isEmpty()) return new HashSet<>();
    return Arrays.stream(list.split(",")).map(RoleResolver::digits).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
  }
}
