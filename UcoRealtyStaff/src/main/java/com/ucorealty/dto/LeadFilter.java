package com.ucorealty.dto;

import java.io.Serializable;

/**
 * What the user typed into the Leads filter. It is kept in the server-side session (never in the address of the page),
 * because a search can be a customer's name or phone number (secure coding order, point 4).
 * Change this file when: the Leads page gets another filter.
 */
public class LeadFilter implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String from;
  private final String to;
  private final String status;
  private final String search;

  public LeadFilter(String from, String to, String status, String search) {
    this.from = clean(from);
    this.to = clean(to);
    this.status = clean(status);
    this.search = clean(search);
  }

  public static LeadFilter empty() { return new LeadFilter(null, null, null, null); }

  private static String clean(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }

  public String getFrom() { return from; }
  public String getTo() { return to; }
  public String getStatus() { return status; }
  public String getSearch() { return search; }
}
