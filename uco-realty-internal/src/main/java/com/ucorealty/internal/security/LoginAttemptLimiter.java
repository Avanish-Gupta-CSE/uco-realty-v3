package com.ucorealty.internal.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptLimiter {
  static final int MAX_FAILURES = 5;
  static final Duration LOCK = Duration.ofMinutes(15);

  private static final class Entry { int failures; Instant lockedUntil; }

  private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();
  private final Clock clock;

  public LoginAttemptLimiter() { this(Clock.systemUTC()); }
  LoginAttemptLimiter(Clock clock) { this.clock = clock; }

  public void checkAllowed(String username) {
    if (username == null) return;
    Entry e = attempts.get(username.toLowerCase());
    if (e != null && e.lockedUntil != null && clock.instant().isBefore(e.lockedUntil)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
          "Too many failed login attempts for this account. Locked for 15 minutes.");
    }
  }

  public boolean isLocked(String username) {
    if (username == null) return false;
    Entry e = attempts.get(username.toLowerCase());
    return e != null && e.lockedUntil != null && clock.instant().isBefore(e.lockedUntil);
  }

  public void recordFailure(String username) {
    if (username == null) return;
    String k = username.toLowerCase();
    attempts.compute(k, (key, cur) -> {
      if (cur == null) cur = new Entry();
      if (cur.lockedUntil != null && clock.instant().isAfter(cur.lockedUntil)) {
        cur.failures = 0;
        cur.lockedUntil = null;
      }
      cur.failures++;
      if (cur.failures >= MAX_FAILURES) {
        cur.lockedUntil = clock.instant().plus(LOCK);
      }
      return cur;
    });
  }

  public void recordSuccess(String username) {
    if (username == null) return;
    attempts.remove(username.toLowerCase());
  }

  public void reset(String username) {
    if (username == null) return;
    attempts.remove(username.toLowerCase());
  }
}

