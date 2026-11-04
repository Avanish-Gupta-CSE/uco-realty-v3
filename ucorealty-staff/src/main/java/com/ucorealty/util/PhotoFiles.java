package com.ucorealty.util;

/** Checks on uploaded photos (real file type from the first bytes, safe file names). */
public final class PhotoFiles {
  private PhotoFiles() {}

  /** Real file type from the first bytes, or null if it is not an allowed type. */
  public static String sniffMime(byte[] b) {
    if (b == null || b.length < 12) return null;
    if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
    if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
    if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
      return "image/webp";
    return null;
  }

  public static boolean extensionMatches(String ext, String mime) {
    if (".png".equals(ext)) return "image/png".equals(mime);
    if (".jpg".equals(ext) || ".jpeg".equals(ext)) return "image/jpeg".equals(mime);
    if (".webp".equals(ext)) return "image/webp".equals(mime);
    return false;
  }

  public static String cleanName(String name) {
    return name.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
