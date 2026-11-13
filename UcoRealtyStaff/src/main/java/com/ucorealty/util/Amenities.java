package com.ucorealty.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Yes/No amenities a maker answers on the project form. To add or rename one, change LIST (and the same list in the
 * customer app's Amenities class). The answers are stored in one column (amenity_flags) as text like
 * "Swimming Pool:Y,Community Hall:N,Gym:Y". Anything else goes in the free-text "amenities" column.
 * Change this file when: the project form gets another Yes/No amenity.
 */
public final class Amenities {
  private Amenities() {}

  public static final List<String> LIST = Collections.unmodifiableList(Arrays.asList(
      "Swimming Pool", "Community Hall", "Gym", "Children's Play Area", "Lift", "24x7 Power Backup", "Parking", "CCTV / Security"));

  /** Reads the radio buttons amenity_0, amenity_1, ... of the form (value Y or N); null when the form had none. */
  public static String fromRequest(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    boolean any = false;
    for (int i = 0; i < LIST.size(); i++) {
      String v = request.getParameter("amenity_" + i);
      if (v != null) any = true;
      if (sb.length() > 0) sb.append(',');
      sb.append(LIST.get(i)).append(':').append("Y".equalsIgnoreCase(v) ? 'Y' : 'N');
    }
    return any ? sb.toString() : null;
  }

  /** "Gym:Y,Lift:N" -> {Gym=Y, Lift=N} in stored order. */
  public static Map<String, String> parse(String flags) {
    Map<String, String> out = new LinkedHashMap<>();
    if (flags == null || flags.isBlank()) return out;
    for (String part : flags.split(",")) {
      int i = part.lastIndexOf(':');
      if (i > 0) out.put(part.substring(0, i).trim(), part.substring(i + 1).trim().toUpperCase());
    }
    return out;
  }
}
