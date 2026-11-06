package com.ucorealty.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Reads the token the bank SSO portal sends to /entry?Value=... (same method as the bank's other projects):
 * Base64 of [12-byte IV][AES/GCM ciphertext + 128-bit tag]; the key is the plain text of the setting, used as raw bytes.
 * After decrypting, the text is: emp_id=61801&sol_id=7115
 */
public final class SsoCryptoUtil {

  private static final int IV_SIZE = 12;
  private static final int TAG_BITS = 128;

  private SsoCryptoUtil() {}

  /** True when the key has a length AES accepts: 16, 24 or 32 characters. */
  public static boolean isValidKey(String key) {
    if (key == null) return false;
    int n = key.getBytes(StandardCharsets.UTF_8).length;
    return n == 16 || n == 24 || n == 32;
  }

  public static String decrypt(String value, String key) throws GeneralSecurityException {
    // A "+" in a URL arrives as a space when the portal has not encoded it
    byte[] decoded = Base64.getDecoder().decode(value.trim().replaceAll("\\s+", "+"));
    if (decoded.length <= IV_SIZE) {
      throw new GeneralSecurityException("Token is too short");
    }
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
        new GCMParameterSpec(TAG_BITS, Arrays.copyOf(decoded, IV_SIZE)));
    return new String(cipher.doFinal(decoded, IV_SIZE, decoded.length - IV_SIZE), StandardCharsets.UTF_8);
  }

  /** The employee number in the decrypted text (emp_id=...&sol_id=...), or null when it is not in that form. */
  public static String employeeNumber(String text) {
    int eq = text.indexOf('=');
    int amp = text.indexOf('&');
    if (eq < 0 || amp <= eq) return null;
    String empNo = text.substring(eq + 1, amp).trim();
    return empNo.matches("[A-Za-z0-9]{1,10}") ? empNo : null;
  }

  /** The SOL in the decrypted text, or an empty string. */
  public static String solId(String text) {
    return text.substring(text.lastIndexOf('=') + 1).trim();
  }
}
