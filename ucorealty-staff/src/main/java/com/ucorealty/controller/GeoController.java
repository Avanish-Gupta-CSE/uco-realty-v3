package com.ucorealty.controller;

import com.ucorealty.security.RoleCheck;
import com.ucorealty.service.MasterDataService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// Full reference lists for staff forms (zones, states, districts). Needs a login.
// Customers use /api/public/meta instead, which lists only places that have approved projects.
@RestController
@RequestMapping("/api/admin/geo")
public class GeoController {
  static final String[] ALL_STAFF = {
    "SUPERADMIN_DIT", "HO_MARKETING", "HO_RETAIL",
    "ZO_MAKER", "ZO_CHECKER"
  };

  private final MasterDataService masterData;

  public GeoController(MasterDataService masterData) { this.masterData = masterData; }

  @GetMapping
  public Map<String, Object> geo(Authentication auth) {
    RoleCheck.require(auth, ALL_STAFF);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("zones", masterData.getZones());
    m.put("states", masterData.getStates());
    m.put("stateDistricts", masterData.getStateDistricts());
    return m;
  }
}
