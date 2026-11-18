package com.ucorealty.util;

import java.time.Duration;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Counts attempts per key (an employee id, an address) over a time window, so that guessing a one-time password or asking for
 * one again and again is stopped (secure coding order, point 3: OTP validation and "resend OTP" are rate limited).
 * A key is blocked while it already has the maximum number of attempts inside the window. The counts live in memory of this
 * application only: a restart clears them.
 * Change this file when: the way attempts are counted changes. The limits themselves are written where it is created (HomeController).
 */
public final class AttemptLimiter {
  private final int max;
  private final long windowMillis;
  private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

  public AttemptLimiter(int max, Duration window) {
    this.max = max;
    this.windowMillis = window.toMillis();
  }

  /** True when this key has used up its attempts inside the window. */
  public boolean isBlocked(String key) {
    return count(key) >= max;
  }

  /** Counts one more attempt for this key. */
  public void record(String key) {
    hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).addLast(System.currentTimeMillis());
    if (hits.size() > 10_000) {   // keep the memory small: forget keys whose attempts are all old
      hits.keySet().removeIf(k -> count(k) == 0);
    }
  }

  /** Forgets the attempts of this key (after a success). */
  public void reset(String key) {
    hits.remove(key);
  }

  private int count(String key) {
    Deque<Long> d = hits.get(key);
    if (d == null) return 0;
    long oldest = System.currentTimeMillis() - windowMillis;
    Long head;
    while ((head = d.peekFirst()) != null && head < oldest) d.pollFirst();
    return d.size();
  }
}
