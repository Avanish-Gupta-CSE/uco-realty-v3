package com.ucorealty.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * UCO Bank Single Sign-On (SSO) Payload Decryption & Encryption Utility.
 * Supports:
 *   1. Bank Production AES-CBC with PKCS5Padding (GetValue.jsp reference from db-login-reference)
 *      with Base64 key and Base64 IV.
 *   2. Bank Production AES-GCM with NoPadding (BranchRecordMaintainance reference)
 *      with 12-byte prepended IV and 128-bit authentication tag.
 */
public final class SsoCryptoUtil {

  private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
  private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
  private static final int GCM_IV_SIZE = 12;
  private static final int GCM_TAG_SIZE = 128;

  private SsoCryptoUtil() {}

  /**
   * Decrypts SSO payload using AES/CBC/PKCS5Padding with Base64 decoded key and IV (GetValue.jsp bank reference).
   */
  public static String decryptAesCbc(String token, String base64Key, String base64Iv) throws Exception {
    if (token == null || token.trim().isEmpty()) {
      throw new IllegalArgumentException("Token cannot be null or empty");
    }
    String normalized = token.replaceAll("\\s+", "+");

    byte[] keyBytes = decodeKeyOrString(base64Key);
    byte[] ivBytes = decodeKeyOrString(base64Iv);

    SecretKeySpec key = new SecretKeySpec(normalizeToKeyLength(keyBytes), "AES");
    IvParameterSpec iv = new IvParameterSpec(normalizeTo16Bytes(ivBytes));

    Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
    cipher.init(Cipher.DECRYPT_MODE, key, iv);

    byte[] cipherText = Base64.getDecoder().decode(normalized);
    byte[] plainText = cipher.doFinal(cipherText);
    return new String(plainText, StandardCharsets.UTF_8);
  }

  /**
   * Encrypts SSO payload using AES/CBC/PKCS5Padding (matching GetValue.jsp specification).
   */
  public static String encryptAesCbc(String plainText, String base64Key, String base64Iv) throws Exception {
    if (plainText == null || plainText.isEmpty()) {
      throw new IllegalArgumentException("Plaintext cannot be null or empty");
    }

    byte[] keyBytes = decodeKeyOrString(base64Key);
    byte[] ivBytes = decodeKeyOrString(base64Iv);

    SecretKeySpec key = new SecretKeySpec(normalizeToKeyLength(keyBytes), "AES");
    IvParameterSpec iv = new IvParameterSpec(normalizeTo16Bytes(ivBytes));

    Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
    cipher.init(Cipher.ENCRYPT_MODE, key, iv);

    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(cipherText);
  }

  /**
   * Dual-mode decryption: attempts AES/CBC/PKCS5Padding first (GetValue.jsp),
   * falling back to AES/GCM/NoPadding (BranchRecordMaintainance).
   */
  public static String decryptPayload(String value, String encryptionKey, String encryptionIv) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Encrypted value cannot be null or empty");
    }

    String normalized = value.replaceAll("\\s+", "+");

    // 1. Try AES/CBC/PKCS5Padding with provided IV (GetValue.jsp)
    if (encryptionIv != null && !encryptionIv.trim().isEmpty()) {
      try {
        return decryptAesCbc(normalized, encryptionKey, encryptionIv);
      } catch (Exception ignored) {
        // Fall through to GCM or fallback CBC
      }
    }

    // 2. Try AES/GCM/NoPadding with prepended 12-byte IV (BranchRecordMaintainance)
    try {
      byte[] decoded = Base64.getDecoder().decode(normalized);
      if (decoded.length > GCM_IV_SIZE) {
        byte[] iv = new byte[GCM_IV_SIZE];
        System.arraycopy(decoded, 0, iv, 0, GCM_IV_SIZE);
        byte[] cipherText = new byte[decoded.length - GCM_IV_SIZE];
        System.arraycopy(decoded, GCM_IV_SIZE, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
        byte[] keyBytes = normalizeKey(encryptionKey);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
      }
    } catch (Exception ignored) {}

    // 3. Fallback: Try AES/CBC without separate IV (using key-derived IV)
    try {
      byte[] keyBytes = normalizeKey(encryptionKey);
      byte[] ivBytes = new byte[16];
      System.arraycopy(keyBytes, 0, ivBytes, 0, 16);
      SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
      IvParameterSpec iv = new IvParameterSpec(ivBytes);

      Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5);
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      byte[] cipherText = Base64.getDecoder().decode(normalized);
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalArgumentException("Token decryption failed under both AES-CBC and AES-GCM: " + e.getMessage(), e);
    }
  }

  /**
   * Backward-compatible overload without IV parameter.
   */
  public static String decryptPayload(String value, String encryptionKey) throws Exception {
    return decryptPayload(value, encryptionKey, null);
  }

  /**
   * Backward-compatible AES-GCM encrypt.
   */
  public static String encryptPayload(String plainText, String encryptionKey) throws Exception {
    if (plainText == null || encryptionKey == null) {
      throw new IllegalArgumentException("Plaintext and key cannot be null");
    }

    byte[] iv = new byte[GCM_IV_SIZE];
    new SecureRandom().nextBytes(iv);

    Cipher cipher = Cipher.getInstance(AES_GCM_NOPADDING);
    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
    byte[] keyBytes = normalizeKey(encryptionKey);
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    byte[] combined = new byte[GCM_IV_SIZE + cipherText.length];
    System.arraycopy(iv, 0, combined, 0, GCM_IV_SIZE);
    System.arraycopy(cipherText, 0, combined, GCM_IV_SIZE, cipherText.length);

    return Base64.getEncoder().encodeToString(combined);
  }

  /**
   * Parses the decrypted plaintext into employee number and SOL ID.
   * Directly implements GetValue.jsp param parsing alongside bank variants.
   */
  public static SsoAuthInfo parseAuthInfo(String decryptedText) {
    if (decryptedText == null || decryptedText.trim().isEmpty()) {
      return null;
    }
    String val = decryptedText.trim();
    String empNo = null;
    String solId = null;

    Map<String, String> params = new HashMap<>();
    if (val.contains("=") || val.contains("&")) {
      try {
        for (String pair : val.split("&")) {
          int idx = pair.indexOf('=');
          if (idx > 0) {
            String k = pair.substring(0, idx).trim().toLowerCase();
            String v = pair.substring(idx + 1).trim();
            params.put(k, v);
          }
        }

        // 1. Exact GetValue.jsp parameters: empno and sol
        if (params.containsKey("empno")) empNo = params.get("empno");
        if (params.containsKey("sol")) solId = params.get("sol");

        // 2. Standard UCO Bank intranet aliases
        if (empNo == null) {
          for (String k : new String[]{"emp_id", "empcode", "empid", "user_id", "username", "uid", "uempno"}) {
            if (params.containsKey(k)) {
              empNo = params.get(k);
              break;
            }
          }
        }

        if (solId == null) {
          for (String k : new String[]{"sol_id", "solid", "branch_id", "branchcode", "usol"}) {
            if (params.containsKey(k)) {
              solId = params.get(k);
              break;
            }
          }
        }
      } catch (Exception ignored) {}
    }

    // Substring fallback
    if (empNo == null && val.contains("=") && val.contains("&")) {
      try {
        empNo = val.substring(val.indexOf("=") + 1, val.indexOf("&")).trim();
        solId = val.substring(val.lastIndexOf("=") + 1).trim();
      } catch (Exception ignored) {}
    }

    // Single value fallback (just employee id)
    if (empNo == null && !val.contains("=") && !val.contains("&")) {
      empNo = val;
    }

    return new SsoAuthInfo(empNo, solId);
  }

  public static class SsoAuthInfo {
    private final String empNo;
    private final String solId;

    public SsoAuthInfo(String empNo, String solId) {
      this.empNo = empNo;
      this.solId = solId;
    }

    public String getEmpNo() { return empNo; }
    public String getSolId() { return solId; }
  }

  private static byte[] decodeKeyOrString(String str) {
    if (str == null || str.trim().isEmpty()) {
      return new byte[16];
    }
    String s = str.trim();
    try {
      byte[] decoded = Base64.getDecoder().decode(s);
      if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
        return decoded;
      }
    } catch (Exception ignored) {}
    return s.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] normalizeToKeyLength(byte[] b) {
    if (b.length == 16 || b.length == 24 || b.length == 32) {
      return b;
    }
    byte[] key = new byte[16];
    System.arraycopy(b, 0, key, 0, Math.min(b.length, 16));
    for (int i = b.length; i < 16; i++) key[i] = (byte) '#';
    return key;
  }

  private static byte[] normalizeTo16Bytes(byte[] b) {
    if (b.length == 16) return b;
    byte[] out = new byte[16];
    System.arraycopy(b, 0, out, 0, Math.min(b.length, 16));
    return out;
  }

  private static byte[] normalizeKey(String key) {
    return normalizeToKeyLength(decodeKeyOrString(key));
  }
}
