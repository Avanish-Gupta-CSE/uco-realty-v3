package com.ucorealty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The two fixed pages of the customer site: Home ("/") and About Us ("/about"). They show no database data. The project list is "/projects"
 * (CatalogPageController).
 * Change this file when: another fixed page (tab) is added.
 */
@Controller
public class StaticPageController {

  @GetMapping("/")
  public String home() {
    return "home";
  }

  @GetMapping("/about")
  public String about() {
    return "about";
  }
}
