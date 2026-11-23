package com.ucorealty.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the Yes/No amenity answers the bank staff saved (amenity_flags, e.g. "Swimming Pool:Y,Gym:N"). The staff app owns the list.
 * Change this file when: the stored amenity text format changes. (The list of amenities is in the staff app: UcoRealtyStaff util/Amenities.)
 */
public final class Amenities {
  private Amenities() {}

  /** The emoji shown in front of each amenity name on the project page. A name that is not listed gets DEFAULT_ICON. (The same map is in the other application: scripts/sync_check.py compares them.) */
  public static final Map<String, String> ICONS = Collections.unmodifiableMap(new LinkedHashMap<String, String>() {{
    put("Swimming Pool", "\uD83C\uDFCA");
    put("Community Hall", "\uD83C\uDFDB\uFE0F");
    put("Gym", "\uD83C\uDFCB\uFE0F");
    put("Children's Play Area", "\uD83C\uDFA0");
    put("Lift", "\uD83D\uDED7");
    put("24x7 Power Backup", "\uD83D\uDD0C");
    put("Parking", "\uD83C\uDD7F\uFE0F");
    put("CCTV / Security", "\uD83D\uDCF9");
  }});

  public static final String DEFAULT_ICON = "\u2728";

  /** "Gym:Y,Lift:N" -> {Gym=Yes, Lift=No} in stored order. (The same method, word for word, is in the staff app: scripts/sync_check.py compares them.) */
  public static Map<String, String> parse(String flags) {
    Map<String, String> out = new LinkedHashMap<>();
    if (flags == null || flags.isBlank()) return out;
    for (String part : flags.split(",")) {
      int i = part.lastIndexOf(':');
      if (i > 0) out.put(part.substring(0, i).trim(), "Y".equalsIgnoreCase(part.substring(i + 1).trim()) ? "Yes" : "No");
    }
    return out;
  }
}
