package com.ucorealty.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds the browser-hardening response headers before anything else runs, so they are present on every
 * response (JSP pages, redirects, 403s, static files). Spring Security's commit-time header writer is not
 * used because small fully-buffered JSP pages (login, error) bypass it.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String CSP =
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self' data:; "
      + "connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("Content-Security-Policy", CSP);
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "same-origin");
    response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
    if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
      response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    }
    if (!isStaticAsset(request)) {
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("Pragma", "no-cache");
    }
    chain.doFilter(request, response);
  }

  private static boolean isStaticAsset(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return path.startsWith("/assets/") || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/");
  }
}
