package com.ucorealty.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Set;

/**
 * Reads the token the bank SSO portal sends to /entry?Value=... (same method as the bank's other projects):
 * Base64 of [IV][ciphertext]; the key is the plain text of sso.encryption.key, used as raw bytes. The cipher is the setting
 * sso.cipher, and only the two values in CIPHERS are accepted: AES/GCM/NoPadding = 12-byte IV + ciphertext with a 128-bit tag
 * (default: the bank's newer applications); AES/CBC/PKCS5Padding = 16-byte IV + ciphertext (no integrity check: use it only
 * when the bank says so).
 * After decrypting, the text is either emp_id=61801&sol_id=7115 or the other bank form xx~61801&yy~7115.
 * Change this file when: the bank changes the token layout (IV size or position) or adds another cipher.
 */
public final class SsoToken {

  public static final String GCM = "AES/GCM/NoPadding";
  public static final String CBC = "AES/CBC/PKCS5Padding";
  private static final Set<String> CIPHERS = Set.of(GCM, CBC);
  private static final int GCM_IV_SIZE = 12;
  private static final int CBC_IV_SIZE = 16;
  private static final int TAG_BITS = 128;

  private SsoToken() {}

  /** True when the key has a length AES accepts: 16, 24 or 32 characters. */
  public static boolean isValidKey(String key) {
    if (key == null) return false;
    int n = key.getBytes(StandardCharsets.UTF_8).length;
    return n == 16 || n == 24 || n == 32;
  }

  /** True when the cipher setting is exactly one of the two supported transformations. */
  public static boolean isValidCipher(String cipher) {
    return cipher != null && CIPHERS.contains(cipher.trim());
  }

  public static String decrypt(String value, String key, String transformation) throws GeneralSecurityException {
    if (!isValidCipher(transformation)) {
      throw new GeneralSecurityException("Unsupported cipher");
    }
    boolean gcm = GCM.equals(transformation.trim());
    int ivSize = gcm ? GCM_IV_SIZE : CBC_IV_SIZE;
    // A "+" in a URL arrives as a space when the portal has not encoded it
    byte[] decoded = LenientBase64.decode(value.trim().replaceAll("\\s+", "+"));
    if (decoded.length <= ivSize) {
      throw new GeneralSecurityException("Token is too short");
    }
    byte[] iv = Arrays.copyOf(decoded, ivSize);
    Cipher cipher = Cipher.getInstance(transformation.trim());
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
        gcm ? new GCMParameterSpec(TAG_BITS, iv) : new IvParameterSpec(iv));
    return new String(cipher.doFinal(decoded, ivSize, decoded.length - ivSize), StandardCharsets.UTF_8);
  }

  /** The employee number in the decrypted text (emp_id=...&sol_id=... or xx~...&yy~...), or null when it is in neither form. */
  public static String employeeNumber(String text) {
    int start = text.indexOf('~') >= 0 ? text.indexOf('~') : text.indexOf('=');
    int amp = text.indexOf('&');
    if (start < 0 || amp <= start) return null;
    String empNo = text.substring(start + 1, amp).trim();
    return empNo.matches("[A-Za-z0-9]{1,10}") ? empNo : null;
  }

  /** The SOL in the decrypted text (after the last "~" or "="), or an empty string. */
  public static String solId(String text) {
    int at = Math.max(text.lastIndexOf('~'), text.lastIndexOf('='));
    return text.substring(at + 1).trim();
  }
}
