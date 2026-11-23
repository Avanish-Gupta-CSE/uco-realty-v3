package com.ucorealty.util;

/**
 * Checks on uploaded photos and PDFs (real file type from the first bytes, safe file names).
 * Change this file when: another file type is allowed for upload.
 */
public final class UploadChecks {
  private UploadChecks() {}

  /** Real file type from the first bytes, or null if it is not an allowed type. */
  public static String sniffMime(byte[] b) {
    if (b == null || b.length < 12) return null;
    if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
    if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
    if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P')
      return "image/webp";
    return null;
  }

  /** True when the bytes start like a real PDF file ("%PDF-"). */
  public static boolean isPdf(byte[] b) {
    return b != null && b.length > 8 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-';
  }

  public static boolean extensionMatches(String ext, String mime) {
    if (".png".equals(ext)) return "image/png".equals(mime);
    if (".jpg".equals(ext) || ".jpeg".equals(ext)) return "image/jpeg".equals(mime);
    if (".webp".equals(ext)) return "image/webp".equals(mime);
    return false;
  }

  /** The name of an uploaded photo as stored: letters, digits and . _ - only, at most 100 characters (the extension is kept). */
  public static String cleanName(String name) {
    return shorten(name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_"), 100);
  }

  /**
   * The name of a PDF attachment as stored and shown to people (it is the link text on the customer page): letters and digits of any
   * language (Hindi too), spaces and . _ - ( ) only; no folder part, no markup characters; at most 100 characters (the extension is kept).
   */
  public static String attachmentName(String name) {
    String n = name == null ? "" : name;
    n = n.substring(Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\')) + 1);                   // no folder part
    n = n.replaceAll("[^\\p{L}\\p{N}\\p{M} ._()-]", "_").replaceAll("\\s+", " ").trim();
    if (n.isEmpty() || n.startsWith(".")) n = "attachment" + n;
    return shorten(n, 100);
  }

  /** The short title of an "Other" document as stored: letters and digits of any language, spaces and . _ ( ) & , / ' - only; at most 100 characters; null when nothing is left. */
  public static String docTitle(String title) {
    String t = title == null ? "" : title.replaceAll("[^\\p{L}\\p{N}\\p{M} ._()&,/'-]", "").replaceAll("\\s+", " ").trim();
    if (t.isEmpty()) return null;
    return t.length() <= 100 ? t : t.substring(0, 100).trim();
  }

  private static String shorten(String n, int max) {
    if (n.length() <= max) return n;
    int dot = n.lastIndexOf('.');
    String ext = dot > 0 && n.length() - dot <= 10 ? n.substring(dot) : "";
    return n.substring(0, max - ext.length()) + ext;
  }
}
