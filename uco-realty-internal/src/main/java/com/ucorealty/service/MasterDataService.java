package com.ucorealty.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MasterDataService {
  private final List<String> zones;
  private final Map<String, List<String>> stateDistricts;

  public MasterDataService(ObjectMapper mapper) throws IOException {
    List<String> z = read(mapper, "geo/zones.json", new TypeReference<List<String>>() {});
    Map<String, List<String>> d = read(mapper, "geo/districts.json",
      new TypeReference<LinkedHashMap<String, List<String>>>() {});
    if (z == null || z.isEmpty() || d == null || d.isEmpty()) {
      throw new IllegalStateException("Reference data in src/main/resources/geo is empty");
    }
    this.zones = Collections.unmodifiableList(new ArrayList<>(z));
    this.stateDistricts = Collections.unmodifiableMap(new LinkedHashMap<>(d));
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
