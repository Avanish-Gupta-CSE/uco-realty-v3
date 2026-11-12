package com.ucorealty.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lets the project form send its ~45 text fields as ONE multipart part.
 *
 * Why: Tomcat (the default in every Tomcat 10.1+/11, including the bank's) refuses a multipart request with more than
 * 50 parts. The project form has about 45 text fields and up to 10 photo inputs, so a normal submit is rejected
 * ("FileCountLimitExceededException", shown to the user as a security error page). The page script (project-form.js)
 * therefore packs every text field into a single urlencoded value, "__packed", and sends only the photos that were
 * chosen as file parts. This filter, which runs after Spring Security has checked the CSRF token, unpacks that value
 * so the controller sees the same request parameters as before.
 */
@Component
public class PackedFormFilter extends OncePerRequestFilter {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PackedFormFilter.class);
  static final String PACKED = "__packed";
  private static final int MAX_FIELDS = 500;   // a form has about 50; refuse anything absurd

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String packed = "POST".equals(request.getMethod()) ? request.getParameter(PACKED) : null;
    if (packed == null) {
      chain.doFilter(request, response);
      return;
    }
    Map<String, String[]> merged = new LinkedHashMap<>(request.getParameterMap());
    merged.remove(PACKED);
    int count = 0;
    for (String pair : packed.split("&")) {
      if (pair.isEmpty()) continue;
      if (++count > MAX_FIELDS) {
        log.warn("[UCO FORM] Refused {} {}: more than {} packed fields", request.getMethod(), request.getRequestURI(), MAX_FIELDS);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Too many fields");
        return;
      }
      int eq = pair.indexOf('=');
      String name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
      String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
      merged.merge(name, new String[] {value}, (a, b) -> {
        String[] all = java.util.Arrays.copyOf(a, a.length + 1);
        all[a.length] = b[0];
        return all;
      });
    }
    log.debug("[UCO FORM] Unpacked {} fields for {}", count, request.getRequestURI());
    chain.doFilter(new Unpacked(request, merged), response);
  }

  private static final class Unpacked extends HttpServletRequestWrapper {
    private final Map<String, String[]> params;

    Unpacked(HttpServletRequest request, Map<String, String[]> params) {
      super(request);
      this.params = Collections.unmodifiableMap(params);
    }

    @Override public String getParameter(String name) {
      String[] v = params.get(name);
      return v == null || v.length == 0 ? null : v[0];
    }
    @Override public String[] getParameterValues(String name) { return params.get(name); }
    @Override public Map<String, String[]> getParameterMap() { return params; }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(params.keySet()); }
  }
}
