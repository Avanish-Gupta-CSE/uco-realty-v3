package com.ucorealty.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * UCO Bank Single Sign-On (SSO) AES-GCM Payload Decryption & Encryption Utility.
 * Aligned with bank production reference project BranchRecordMaintainance (HomeController.java).
 */
public final class SsoCryptoUtil {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_SIZE = 12; // 12-byte IV for GCM
  private static final int TAG_SIZE = 128; // 128-bit authentication tag

  private SsoCryptoUtil() {}

  /**
   * Decrypts the Base64-encoded AES-GCM encrypted payload received in /entry?value=...
   */
  public static String decryptPayload(String value, String encryptionKey) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Encrypted value cannot be null or empty");
    }
    if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Encryption key cannot be null or empty");
    }

    // Replace whitespace/newlines with '+' as per bank reference implementation
    value = value.replaceAll("\\s+", "+");
    byte[] decoded = Base64.getDecoder().decode(value);

    if (decoded.length < IV_SIZE) {
      throw new IllegalArgumentException("Decoded ciphertext is too short to contain IV");
    }

    byte[] iv = new byte[IV_SIZE];
    System.arraycopy(decoded, 0, iv, 0, IV_SIZE);

    byte[] cipherText = new byte[decoded.length - IV_SIZE];
    System.arraycopy(decoded, IV_SIZE, cipherText, 0, cipherText.length);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
    byte[] keyBytes = normalizeKey(encryptionKey);
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

    byte[] plainText = cipher.doFinal(cipherText);
    return new String(plainText, StandardCharsets.UTF_8);
  }

  /**
   * Encrypts plaintext payload (e.g. "emp_id=61801&sol_id=7115") for test generation.
   */
  public static String encryptPayload(String plainText, String encryptionKey) throws Exception {
    if (plainText == null || encryptionKey == null) {
      throw new IllegalArgumentException("Plaintext and key cannot be null");
    }

    byte[] iv = new byte[IV_SIZE];
    new SecureRandom().nextBytes(iv);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
    byte[] keyBytes = normalizeKey(encryptionKey);
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);

    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    byte[] combined = new byte[IV_SIZE + cipherText.length];
    System.arraycopy(iv, 0, combined, 0, IV_SIZE);
    System.arraycopy(cipherText, 0, combined, IV_SIZE, cipherText.length);

    return Base64.getEncoder().encodeToString(combined);
  }

  /**
   * Parses the decrypted plaintext into employee number and SOL ID.
   * Handles bank pattern: val.substring(val.indexOf("=") + 1, val.indexOf("&"))
   * as well as standard URL query parameter pairs.
   */
  public static SsoAuthInfo parseAuthInfo(String decryptedText) {
    if (decryptedText == null || decryptedText.trim().isEmpty()) {
      return null;
    }
    String val = decryptedText.trim();
    String empNo = null;
    String solId = null;

    // Check bank substring pattern: emp_id=61801&sol_id=7115
    if (val.contains("=") && val.contains("&")) {
      try {
        Map<String, String> params = new HashMap<>();
        String[] pairs = val.split("&");
        for (String pair : pairs) {
          int idx = pair.indexOf("=");
          if (idx > 0) {
            String k = pair.substring(0, idx).trim().toLowerCase();
            String v = pair.substring(idx + 1).trim();
            params.put(k, v);
          }
        }

        // Look for employee ID keys
        for (String k : new String[]{"emp_id", "empno", "empcode", "empid", "user_id", "username"}) {
          if (params.containsKey(k)) {
            empNo = params.get(k);
            break;
          }
        }

        // Look for SOL ID keys
        for (String k : new String[]{"sol_id", "solid", "sol", "branch_id", "branchcode"}) {
          if (params.containsKey(k)) {
            solId = params.get(k);
            break;
          }
        }
      } catch (Exception ignored) {}
    }

    // Fallback to exact bank reference substring logic if params couldn't be parsed
    if (empNo == null && val.contains("=") && val.contains("&")) {
      try {
        empNo = val.substring(val.indexOf("=") + 1, val.indexOf("&")).trim();
        solId = val.substring(val.lastIndexOf("=") + 1).trim();
      } catch (Exception ignored) {}
    }

    // Single value fallback (just empId)
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

    public boolean isValid() {
      return empNo != null && !empNo.trim().isEmpty();
    }
  }

  private static byte[] normalizeKey(String key) {
    byte[] b = key.getBytes(StandardCharsets.UTF_8);
    if (b.length == 16 || b.length == 24 || b.length == 32) {
      return b;
    }
    // Pad or truncate to 16 bytes for standard AES-128
    byte[] key16 = new byte[16];
    System.arraycopy(b, 0, key16, 0, Math.min(b.length, 16));
    for (int i = b.length; i < 16; i++) {
      key16[i] = (byte) '#';
    }
    return key16;
  }
}

