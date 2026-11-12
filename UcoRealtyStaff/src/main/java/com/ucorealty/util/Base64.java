package com.ucorealty.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * Small Base64 decoder, same idea as the Base64 class in the bank's other projects (com.uco.util.Base64):
 * ignores spaces and line breaks, accepts both the standard (+ /) and the URL-safe (- _) letters, does not need the
 * "=" padding, and unzips the result when it is gzip-compressed. Decode only; the SSO token is never produced here.
 */
public final class Base64 {

  private static final byte WHITESPACE = -5;
  private static final byte INVALID = -9;
  private static final byte[] DECODABET = new byte[128];

  static {
    Arrays.fill(DECODABET, INVALID);
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    for (int i = 0; i < alphabet.length(); i++) DECODABET[alphabet.charAt(i)] = (byte) i;
    DECODABET['-'] = 62;   // URL-safe forms of + and /
    DECODABET['_'] = 63;
    for (char c : new char[] {' ', '\t', '\n', '\r'}) DECODABET[c] = WHITESPACE;
  }

  private Base64() {}

  /** Decodes Base64 text to bytes; throws IllegalArgumentException when the text is not valid Base64. */
  public static byte[] decode(String text) {
    if (text == null) throw new IllegalArgumentException("No Base64 text");
    byte[] in = text.getBytes(StandardCharsets.ISO_8859_1);
    ByteArrayOutputStream out = new ByteArrayOutputStream(in.length * 3 / 4);
    int buffer = 0;
    int bits = 0;
    for (byte b : in) {
      if (b == '=') break;                       // padding: the data is finished
      byte v = b >= 0 ? DECODABET[b] : INVALID;
      if (v == WHITESPACE) continue;
      if (v == INVALID) throw new IllegalArgumentException("Not valid Base64 text");
      buffer = (buffer << 6) | v;
      bits += 6;
      if (bits >= 8) {
        bits -= 8;
        out.write((buffer >> bits) & 0xFF);
        buffer &= (1 << bits) - 1;
      }
    }
    byte[] bytes = out.toByteArray();
    return isGzip(bytes) ? gunzip(bytes) : bytes;
  }

  private static boolean isGzip(byte[] b) {
    return b.length > 2 && b[0] == (byte) 0x1f && b[1] == (byte) 0x8b;
  }

  private static byte[] gunzip(byte[] b) {
    try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(b))) {
      return gz.readAllBytes();
    } catch (IOException e) {
      return b;   // not really gzip: keep the bytes as they are
    }
  }
}
