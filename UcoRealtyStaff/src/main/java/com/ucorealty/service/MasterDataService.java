package com.ucorealty.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The fixed lists the forms need: the 49 zones and, for each state, its districts. Read once at start from
 * src/main/resources/geo/zones.json and districts.json.
 * Change this file when: the lists are read from somewhere else. To add a zone or district, edit the JSON files.
 */
@Service
public class MasterDataService {
  private static final Logger log = LoggerFactory.getLogger(MasterDataService.class);
  private final List<String> zones;
  private final Map<String, List<String>> stateDistricts;

  public MasterDataService() throws IOException {
    this(new ObjectMapper());
  }

  public MasterDataService(ObjectMapper mapper) throws IOException {
    if (mapper == null) {
      mapper = new ObjectMapper();
    }
    List<String> z = read(mapper, "geo/zones.json", new TypeReference<List<String>>() {});
    Map<String, List<String>> d = read(mapper, "geo/districts.json",
      new TypeReference<LinkedHashMap<String, List<String>>>() {});
    if (z == null || z.isEmpty() || d == null || d.isEmpty()) {
      throw new IllegalStateException("Reference data in src/main/resources/geo is empty");
    }
    this.zones = Collections.unmodifiableList(new ArrayList<>(z));
    this.stateDistricts = Collections.unmodifiableMap(new LinkedHashMap<>(d));
    log.info("[UCO MASTER] Reference data loaded: {} zones, {} states", zones.size(), stateDistricts.size());
  }

  private static <T> T read(ObjectMapper mapper, String path, TypeReference<T> type) throws IOException {
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return mapper.readValue(in, type);
    }
  }

  public List<String> getZones() { return zones; }

  public Map<String, List<String>> getStateDistricts() { return stateDistricts; }

  public List<String> getStates() {
    return stateDistricts.keySet().stream().sorted().collect(Collectors.toList());
  }
}
