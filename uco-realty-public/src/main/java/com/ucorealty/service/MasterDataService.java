package com.ucorealty.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/** State list for the catalog filter, read once from geo/districts.json. */
@Service
public class MasterDataService {
  private final List<String> states;

  public MasterDataService() throws IOException {
    Map<String, List<String>> d;
    try (InputStream in = new ClassPathResource("geo/districts.json").getInputStream()) {
      d = new ObjectMapper().readValue(in, new TypeReference<LinkedHashMap<String, List<String>>>() {});
    }
    if (d == null || d.isEmpty()) {
      throw new IllegalStateException("Reference data in src/main/resources/geo is empty");
    }
    this.states = Collections.unmodifiableList(d.keySet().stream().sorted().collect(Collectors.toList()));
  }

  public List<String> getStates() { return states; }
}
