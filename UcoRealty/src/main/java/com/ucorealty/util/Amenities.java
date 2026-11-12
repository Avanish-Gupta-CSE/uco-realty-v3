package com.ucorealty.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the Yes/No amenity answers the bank staff saved (amenity_flags, e.g. "Swimming Pool:Y,Gym:N"). The staff app owns the list. */
public final class Amenities {
  private Amenities() {}

  /** "Gym:Y,Lift:N" -> {Gym=Yes, Lift=No} in stored order. */
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
