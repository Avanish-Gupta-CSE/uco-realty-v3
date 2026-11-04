package com.ucorealty.service;

import com.ucorealty.entity.ProjectData;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Format checks for the details a maker types in. The same rules are repeated on the screen
// (frontend/src/validation/project.ts) so the maker sees the problem at once; this is the version that cannot be skipped.
// What is NOT checked here: which fields are mandatory (that depends on the business, see Open_Questions B3).
public final class ProjectValidator {
  private ProjectValidator() {}

  // Longest text each column can hold (taken from db_migrations/01_CREATE_TABLES.sql; a test keeps them in step).
  static final Map<String, Integer> MAX = new LinkedHashMap<>();
  static {
    for (String f : new String[] {"zoneName", "branchName", "stateName", "cityName", "propertyId", "typeOfUnit", "totalProjectArea",
        "distRailway", "distAirport", "avgRate", "budgetMin", "budgetMax", "approvalSought"}) MAX.put(f, 100);
    for (String f : new String[] {"reraNo"}) MAX.put(f, 100);
    for (String f : new String[] {"carpetMin", "carpetMax", "buildupMin", "buildupMax", "builderRating"}) MAX.put(f, 50);
    for (String f : new String[] {"promoter1", "promoter2", "promoter3", "promoter4", "promoter5", "salesExecName",
        "branchHeadName", "relExecName", "builderName", "salesEmail", "website"}) MAX.put(f, 255);
    for (String f : new String[] {"salesMobile", "branchHeadNumber", "relExecNumber"}) MAX.put(f, 15);
    MAX.put("projectName", 255);
    MAX.put("projectAddress", 500);
    MAX.put("renewalPending", 20);
    MAX.put("pinCode", 10);
  }

  private static final Pattern RERA = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 /._-]{2,38}[A-Za-z0-9]$");
  private static final Pattern PHONE_CHARS = Pattern.compile("^[0-9+()\\- ]+$");
  private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
  private static final Pattern PIN = Pattern.compile("^[1-9][0-9]{5}$");
  private static final Pattern LEADING_NUMBER = Pattern.compile("^\\s*[₹]?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([A-Za-z]*)");
  private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
  private static final LocalDate LATEST = LocalDate.of(2100, 12, 31);

  private static String s(String v) { return v == null ? "" : v.trim(); }

  /** Every problem found, as short sentences a maker can act on. Empty list means the details are well formed. */
  public static List<String> problems(ProjectData p) {
    List<String> out = new ArrayList<>();

    // Free text is shown on many screens: markup characters are refused on input (screens also HTML-escape on output)
    for (java.lang.reflect.Field f : ProjectData.businessFields()) {
      Object v = ProjectData.value(p, f);
      if (v instanceof String && (((String) v).indexOf('<') >= 0 || ((String) v).indexOf('>') >= 0))
        out.add(label(f.getName()) + " must not contain the characters < or >");
    }

    if (s(p.projectName).length() < 3) out.add("Project name must be at least 3 characters");
    lengths(p, out);

    if (!s(p.pinCode).isEmpty() && !PIN.matcher(s(p.pinCode)).matches()) out.add("Pincode must be 6 digits and cannot start with 0");

    String rera = s(p.reraNo);
    if (!rera.isEmpty() && !rera.equalsIgnoreCase("Not applicable") && !RERA.matcher(rera).matches())
      out.add("RERA number may contain only letters, digits, spaces and / . _ - (4 to 40 characters), or write \"Not applicable\"");

    phone("Sales mobile", p.salesMobile, out);
    phone("Branch head number", p.branchHeadNumber, out);
    phone("Relationship executive number", p.relExecNumber, out);

    if (!s(p.salesEmail).isEmpty() && !EMAIL.matcher(s(p.salesEmail)).matches()) out.add("Sales email is not a valid email address");
    if (!s(p.website).isEmpty() && !validUrl(s(p.website))) out.add("Website must be a full address starting with http:// or https://");

    // numbers
    if (p.totalInventory != null && (p.totalInventory < 1 || p.totalInventory > 100000)) out.add("Total inventory must be between 1 and 100000");
    if (p.unsoldInventory != null && p.unsoldInventory < 0) out.add("Available inventory cannot be negative");
    if (p.unsoldInventory != null && p.totalInventory != null && p.unsoldInventory > p.totalInventory)
      out.add("Available inventory cannot exceed total inventory");
    if (p.aptPerFloor != null && (p.aptPerFloor < 1 || p.aptPerFloor > 100)) out.add("Apartments per floor must be between 1 and 100");
    if (p.noOfTowers != null && (p.noOfTowers < 1 || p.noOfTowers > 500)) out.add("Number of towers must be between 1 and 500");

    // ranges typed as text with units ("650 sq.ft.", "38.0 L", "1.20 Cr")
    range("Carpet area", p.carpetMin, p.carpetMax, false, out);
    range("Built-up area", p.buildupMin, p.buildupMax, false, out);
    range("Budget", p.budgetMin, p.budgetMax, true, out);
    mustHaveDigit("Total project area", p.totalProjectArea, out);
    mustHaveDigit("Distance to railway station", p.distRailway, out);
    mustHaveDigit("Distance to airport", p.distAirport, out);
    mustHaveDigit("Average rate", p.avgRate, out);

    // dates
    date("Approval date", p.approvalDate, true, out);
    date("Inventory as-on date", p.inventoryAsOnDate, true, out);
    date("Project start date", p.startDate, false, out);
    date("Completion date", p.completionDate, false, out);
    date("Valid-upto date", p.validUpto, false, out);
    if (p.startDate != null && p.completionDate != null && p.completionDate.isBefore(p.startDate))
      out.add("Completion date cannot be before the start date");
    if (p.approvalDate != null && p.validUpto != null && p.validUpto.isBefore(p.approvalDate))
      out.add("Valid-upto date cannot be before the approval date");

    if (s(p.amenities).length() > 4000) out.add("Amenities is too long (4000 characters at most)");
    if (s(p.builderBg).length() > 4000) out.add("Builder background is too long (4000 characters at most)");
    return out;
  }

  private static void lengths(ProjectData p, List<String> out) {
    Map<String, String> v = new LinkedHashMap<>();
    v.put("zoneName", p.zoneName); v.put("branchName", p.branchName); v.put("stateName", p.stateName); v.put("cityName", p.cityName);
    v.put("projectName", p.projectName); v.put("reraNo", p.reraNo); v.put("propertyId", p.propertyId); v.put("typeOfUnit", p.typeOfUnit);
    v.put("totalProjectArea", p.totalProjectArea); v.put("carpetMin", p.carpetMin); v.put("carpetMax", p.carpetMax);
    v.put("buildupMin", p.buildupMin); v.put("buildupMax", p.buildupMax); v.put("distRailway", p.distRailway);
    v.put("distAirport", p.distAirport); v.put("avgRate", p.avgRate); v.put("budgetMin", p.budgetMin); v.put("budgetMax", p.budgetMax);
    v.put("builderName", p.builderName); v.put("projectAddress", p.projectAddress); v.put("pinCode", p.pinCode);
    v.put("promoter1", p.promoter1); v.put("promoter2", p.promoter2); v.put("promoter3", p.promoter3);
    v.put("promoter4", p.promoter4); v.put("promoter5", p.promoter5); v.put("salesExecName", p.salesExecName);
    v.put("salesMobile", p.salesMobile); v.put("salesEmail", p.salesEmail); v.put("website", p.website);
    v.put("builderRating", p.builderRating); v.put("branchHeadName", p.branchHeadName); v.put("branchHeadNumber", p.branchHeadNumber);
    v.put("relExecName", p.relExecName); v.put("relExecNumber", p.relExecNumber); v.put("renewalPending", p.renewalPending);
    v.put("approvalSought", p.approvalSought);
    v.forEach((field, value) -> {
      Integer max = MAX.get(field);
      if (max != null && value != null && value.length() > max) out.add(label(field) + " is too long (" + max + " characters at most)");
    });
  }

  private static String label(String field) {
    return field.replaceAll("([A-Z])", " $1").toLowerCase().replaceFirst("^.", String.valueOf(Character.toUpperCase(field.charAt(0))));
  }

  private static void phone(String name, String value, List<String> out) {
    String v = s(value);
    if (v.isEmpty()) return;
    long digits = v.chars().filter(Character::isDigit).count();
    if (!PHONE_CHARS.matcher(v).matches() || digits < 8 || digits > 13)
      out.add(name + " must be a phone number (8 to 13 digits; you may use + ( ) - and spaces)");
  }

  private static boolean validUrl(String u) {
    try {
      URI uri = new URI(u);
      return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null && uri.getHost().contains(".");
    } catch (Exception e) {
      return false;
    }
  }

  private static void mustHaveDigit(String name, String value, List<String> out) {
    if (!s(value).isEmpty() && s(value).chars().noneMatch(Character::isDigit)) out.add(name + " must include a number");
  }

  private static void date(String name, LocalDate d, boolean notInFuture, List<String> out) {
    if (d == null) return;
    if (d.isBefore(EARLIEST) || d.isAfter(LATEST)) out.add(name + " is not a sensible date");
    else if (notInFuture && d.isAfter(LocalDate.now())) out.add(name + " cannot be in the future");
  }

  /** Number in rupees for "38.0 L", "1.20 Cr", "₹ 45,00,000"; square feet or plain numbers when money is false. Null if there is no number. */
  static BigDecimal amount(String text, boolean money) {
    Matcher m = LEADING_NUMBER.matcher(s(text));
    if (!m.find()) return null;
    BigDecimal n = new BigDecimal(m.group(1).replace(",", ""));
    if (!money) return n;
    String unit = m.group(2).toLowerCase();
    if (unit.startsWith("cr")) return n.multiply(BigDecimal.valueOf(10_000_000));
    if (unit.equals("l") || unit.startsWith("lac") || unit.startsWith("lak")) return n.multiply(BigDecimal.valueOf(100_000));
    if (unit.equals("k")) return n.multiply(BigDecimal.valueOf(1_000));
    return n;
  }

  private static void range(String name, String min, String max, boolean money, List<String> out) {
    if (!s(min).isEmpty() && amount(min, money) == null) out.add(name + " minimum must start with a number");
    if (!s(max).isEmpty() && amount(max, money) == null) out.add(name + " maximum must start with a number");
    BigDecimal a = amount(min, money), b = amount(max, money);
    if (a != null && b != null && a.compareTo(b) > 0) out.add(name + " minimum cannot be more than the maximum");
  }
}
