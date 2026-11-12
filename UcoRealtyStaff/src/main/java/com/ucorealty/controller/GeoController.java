package com.ucorealty.controller;

import com.ucorealty.service.MasterDataService;
import com.ucorealty.util.RoleCheck;
import com.ucorealty.util.Roles;
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
  private final MasterDataService masterData;

  public GeoController(MasterDataService masterData) { this.masterData = masterData; }

  @GetMapping
  public Map<String, Object> geo(Authentication auth) {
    RoleCheck.require(auth, Roles.ALL_STAFF);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("zones", masterData.getZones());
    m.put("states", masterData.getStates());
    m.put("stateDistricts", masterData.getStateDistricts());
    return m;
  }
}
