package com.ucorealty.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The document slots of a project (stored in realty_project_attachments.doc_type): three named PDFs, each optional, and OTHER (any number,
 * each with a short title the maker types, stored in doc_title). The customer page shows the label (Brochure, Floor Plan, ...) instead of the file name.
 * To add a slot: add it to SLOTS here AND in the other application (scripts/sync_check.py compares them), to the CHECK constraint
 * chk_realty_att_doctype in db_migrations 01 (and an ALTER for existing databases), and a row on the staff project form.
 * Change this file when: a document slot is added, removed or renamed.
 */
public final class DocTypes {
  private DocTypes() {}

  public static final String OTHER = "OTHER";

  /** The named slots in the order they are shown: code -> label. */
  public static final Map<String, String> SLOTS = Collections.unmodifiableMap(new LinkedHashMap<String, String>() {{
    put("BROCHURE", "Brochure");
    put("FLOOR_PLAN", "Floor Plan");
    put("RERA", "RERA Certificate");
  }});

  public static boolean isSlot(String code) {
    return code != null && SLOTS.containsKey(code);
  }

  /** Sort position: the named slots in their order, then everything else. */
  public static int order(String code) {
    int i = 0;
    for (String slot : SLOTS.keySet()) {
      if (slot.equals(code)) return i;
      i++;
    }
    return SLOTS.size();
  }

  /** The text shown to people: the slot's label; for OTHER the maker's title, or the file name without ".pdf" when there is no title. */
  public static String display(String docType, String docTitle, String fileName) {
    if (isSlot(docType)) return SLOTS.get(docType);
    if (docTitle != null && !docTitle.isBlank()) return docTitle;
    if (fileName == null || fileName.isBlank()) return "Document";
    return fileName.replaceAll("(?i)\\.pdf$", "");
  }
}
