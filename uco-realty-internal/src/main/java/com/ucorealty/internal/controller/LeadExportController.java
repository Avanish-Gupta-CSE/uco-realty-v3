package com.ucorealty.internal.controller;

import com.ucorealty.entity.Lead;
import com.ucorealty.repository.LeadRepository;
import com.ucorealty.internal.security.RoleCheck;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lead Management & Export for Head Office Marketing, Retail Loan Hubs, and Call Centre.
 */
@RestController
@RequestMapping("/api/admin/leads")
public class LeadExportController {
  private final LeadRepository leads;

  static final String[] LEAD_ROLES = {"CALL_CENTRE", "HO_ADMIN", "HO_MARKETING", "HO_RETAIL", "SUPERADMIN_DIT"};

  public LeadExportController(LeadRepository leads) {
    this.leads = leads;
  }

  /**
   * Filter leads with optional date range, lead status, and keyword search.
   */
  @GetMapping
  public List<Lead> list(@RequestParam(required = false) String from,
                         @RequestParam(required = false) String to,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String search,
                         Authentication auth) {
    RoleCheck.require(auth, LEAD_ROLES);
    return filterLeads(from, to, status, search);
  }

  /**
   * Export filtered leads to a beautifully styled Excel (.xlsx) file with Call Center action columns.
   */
  @GetMapping("/export.xlsx")
  public void export(@RequestParam(required = false) String from,
                     @RequestParam(required = false) String to,
                     @RequestParam(required = false) String status,
                     @RequestParam(required = false) String search,
                     HttpServletResponse res,
                     Authentication auth) throws IOException {
    String exporter = RoleCheck.require(auth, LEAD_ROLES);

    List<Lead> filtered = filterLeads(from, to, status, search);

    // Build dynamic date-based filename
    String filename = "uco-realty-leads";
    if (from != null && !from.trim().isEmpty() && to != null && !to.trim().isEmpty()) {
      filename += "_" + from.replaceAll("[^0-9-]", "") + "_to_" + to.replaceAll("[^0-9-]", "");
    } else if (from != null && !from.trim().isEmpty()) {
      filename += "_from_" + from.replaceAll("[^0-9-]", "");
    } else if (to != null && !to.trim().isEmpty()) {
      filename += "_upto_" + to.replaceAll("[^0-9-]", "");
    }
    filename += ".xlsx";

    res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sh = wb.createSheet("Call Centre Leads");

      // Cell Styles
      CellStyle titleStyle = wb.createCellStyle();
      Font titleFont = wb.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 13);
      titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
      titleStyle.setFont(titleFont);

      CellStyle metaStyle = wb.createCellStyle();
      Font metaFont = wb.createFont();
      metaFont.setItalic(true);
      metaFont.setFontHeightInPoints((short) 9);
      metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
      metaStyle.setFont(metaFont);

      CellStyle headerStyle = wb.createCellStyle();
      Font headerFont = wb.createFont();
      headerFont.setBold(true);
      headerFont.setColor(IndexedColors.WHITE.getIndex());
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      headerStyle.setBorderBottom(BorderStyle.MEDIUM);
      headerStyle.setBorderTop(BorderStyle.MEDIUM);

      CellStyle actionHeaderStyle = wb.createCellStyle();
      Font actionHeaderFont = wb.createFont();
      actionHeaderFont.setBold(true);
      actionHeaderFont.setColor(IndexedColors.WHITE.getIndex());
      actionHeaderStyle.setFont(actionHeaderFont);
      actionHeaderStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
      actionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      actionHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
      actionHeaderStyle.setBorderBottom(BorderStyle.MEDIUM);
      actionHeaderStyle.setBorderTop(BorderStyle.MEDIUM);

      CellStyle dataStyle = wb.createCellStyle();
      dataStyle.setBorderBottom(BorderStyle.THIN);
      dataStyle.setBorderTop(BorderStyle.THIN);
      dataStyle.setBorderLeft(BorderStyle.THIN);
      dataStyle.setBorderRight(BorderStyle.THIN);

      CellStyle altDataStyle = wb.createCellStyle();
      altDataStyle.cloneStyleFrom(dataStyle);
      altDataStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      altDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      CellStyle dateStyle = wb.createCellStyle();
      dateStyle.cloneStyleFrom(dataStyle);
      dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

      CellStyle badgeStyle = wb.createCellStyle();
      badgeStyle.cloneStyleFrom(dataStyle);
      badgeStyle.setAlignment(HorizontalAlignment.CENTER);
      Font badgeFont = wb.createFont();
      badgeFont.setBold(true);
      badgeStyle.setFont(badgeFont);

      // Row 0: Sheet Title
      Row titleRow = sh.createRow(0);
      Cell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("UCO BANK - UCO REALTY CUSTOMER LEADS & HOME LOAN ENQUIRIES");
      titleCell.setCellStyle(titleStyle);

      // Row 1: Subtitle / Filter Metadata
      Row metaRow = sh.createRow(1);
      String dateBanner = "Date Filter: " + (from != null && !from.trim().isEmpty() ? from : "Beginning") +
                          " to " + (to != null && !to.trim().isEmpty() ? to : "Present") +
                          " | Status Filter: " + (status != null && !status.trim().isEmpty() ? status : "ALL") +
                          " | Total Leads: " + filtered.size() +
                          " | Exported By: " + exporter + " | Handover Time: " +
                          LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
      Cell metaCell = metaRow.createCell(0);
      metaCell.setCellValue(dateBanner);
      metaCell.setCellStyle(metaStyle);

      // Row 3: Column Headers
      Row h = sh.createRow(3);
      h.setHeightInPoints(24);
      String[] cols = {
        "Lead ID", "Customer Name", "Phone Number", "Project ID",
        "Lead Status", "Date", "Notes"
      };

      for (int i = 0; i < cols.length; i++) {
        Cell c = h.createCell(i);
        c.setCellValue(cols[i]);
        if (i == 6) {
          c.setCellStyle(actionHeaderStyle);
        } else {
          c.setCellStyle(headerStyle);
        }
      }

      // Data Rows
      int rowIdx = 4;
      for (Lead l : filtered) {
        Row r = sh.createRow(rowIdx);
        CellStyle rowStyle = (rowIdx % 2 == 0) ? altDataStyle : dataStyle;

        // 0: ID
        Cell c0 = r.createCell(0);
        c0.setCellValue(l.id != null ? l.id : 0);
        c0.setCellStyle(rowStyle);

        // 1: Name
        Cell c1 = r.createCell(1);
        c1.setCellValue(l.customerName != null ? l.customerName : "");
        c1.setCellStyle(rowStyle);

        // 2: Phone
        Cell c2 = r.createCell(2);
        c2.setCellValue(l.phone != null ? l.phone : "");
        c2.setCellStyle(rowStyle);

        // 3: Project
        Cell c3 = r.createCell(3);
        c3.setCellValue(l.projectId != null ? l.projectId : "-");
        c3.setCellStyle(rowStyle);

        // 4: Status
        String displayStatus = "Visitor";
        if ("Showed interest in Home Loan".equalsIgnoreCase(l.status) || "SUCCESSFUL_APPLIED".equalsIgnoreCase(l.status)) {
          displayStatus = "Showed interest in Home Loan";
        } else if (l.status != null && !l.status.trim().isEmpty()) {
          displayStatus = l.status;
        }
        Cell c4 = r.createCell(4);
        c4.setCellValue(displayStatus);
        c4.setCellStyle(badgeStyle);

        // 5: Date
        Cell c5 = r.createCell(5);
        c5.setCellValue(l.getFormattedDate());
        c5.setCellStyle(rowStyle);

        // 6: Notes (blank action column)
        Cell c6 = r.createCell(6);
        c6.setCellValue("");
        c6.setCellStyle(rowStyle);

        rowIdx++;
      }

      // Auto-fit column widths
      for (int i = 0; i < cols.length; i++) {
        sh.autoSizeColumn(i);
        sh.setColumnWidth(i, Math.min(256 * 40, sh.getColumnWidth(i) + 1200));
      }

      wb.write(res.getOutputStream());
    }
  }

  private List<Lead> filterLeads(String from, String to, String status, String search) {
    LocalDate fromDate = null;
    LocalDate toDate = null;
    try {
      if (from != null && !from.trim().isEmpty()) fromDate = LocalDate.parse(from);
      if (to != null && !to.trim().isEmpty()) toDate = LocalDate.parse(to);
    } catch (java.time.format.DateTimeParseException e) {
      throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST, "Dates must be in yyyy-MM-dd format");
    }

    final LocalDate fFrom = fromDate;
    final LocalDate fTo = toDate;

    return leads.findAll().stream()
      .filter(l -> {
        if (l.createdAt == null) return true;
        LocalDate leadDate = l.createdAt.toLocalDate();
        if (fFrom != null && leadDate.isBefore(fFrom)) return false;
        if (fTo != null && leadDate.isAfter(fTo)) return false;
        return true;
      })
      .filter(l -> {
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
        if (status.equalsIgnoreCase(l.status)) return true;
        if ("Visitor".equalsIgnoreCase(status) && "VISITOR".equalsIgnoreCase(l.status)) return true;
        if ("Showed interest in Home Loan".equalsIgnoreCase(status) && "SUCCESSFUL_APPLIED".equalsIgnoreCase(l.status)) return true;
        return false;
      })
      .filter(l -> {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.toLowerCase();
        return (l.customerName != null && l.customerName.toLowerCase().contains(q))
            || (l.phone != null && l.phone.contains(q))
            || (l.projectId != null && l.projectId.toLowerCase().contains(q))
            || (l.branchSelected != null && l.branchSelected.toLowerCase().contains(q));
      })
      .sorted((a, b) -> {
        if (a.createdAt == null || b.createdAt == null) return 0;
        return b.createdAt.compareTo(a.createdAt); // Newest first
      })
      .collect(Collectors.toList());
  }
}
