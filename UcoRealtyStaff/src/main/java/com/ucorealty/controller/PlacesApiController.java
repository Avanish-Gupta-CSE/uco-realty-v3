package com.ucorealty.controller;

import com.ucorealty.service.MasterDataService;
import com.ucorealty.util.StaffRoles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The full lists of zones, states and districts that the staff project form needs (JSON, needs a login).
 * Customers use /api/public/meta instead, which lists only the places that have approved projects.
 * Change this file when: the form needs another reference list (the data is read by MasterDataService).
 */
@RestController
@RequestMapping("/api/admin/geo")
public class PlacesApiController {
  private final MasterDataService masterData;

  public PlacesApiController(MasterDataService masterData) { this.masterData = masterData; }

  @GetMapping
  public Map<String, Object> geo(Authentication auth) {
    StaffRoles.require(auth, StaffRoles.ALL_STAFF);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("zones", masterData.getZones());
    m.put("states", masterData.getStates());
    m.put("stateDistricts", masterData.getStateDistricts());
    return m;
  }
}
