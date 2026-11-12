package com.ucorealty.controller;

import com.ucorealty.dto.LeadRow;
import com.ucorealty.hrms.LgEntryRepository;
import com.ucorealty.security.RoleCheck;
import com.ucorealty.security.Roles;
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

/**
 * Lead list + Excel export for Head Office Marketing, Retail Banking and DIT.
 * The leads are NOT stored by us: the bank's Home Loan page keeps them in HRMS table LG_ENTRY_REVAMP (CHANNEL = 'REALTY'),
 * and LgEntryRepository reads them from there.
 */
@RestController
@RequestMapping("/api/admin/leads")
public class LeadExportController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeadExportController.class);
  private final LgEntryRepository leads;


  public LeadExportController(LgEntryRepository leads) {
    this.leads = leads;
  }

  /**
   * Filter leads with optional date range, lead status, and keyword search.
   */
  @GetMapping
  public List<LeadRow> list(@RequestParam(required = false) String from,
                         @RequestParam(required = false) String to,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String search,
                         Authentication auth) {
    RoleCheck.require(auth, Roles.LEAD_ROLES);
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
    String exporter = RoleCheck.require(auth, Roles.LEAD_ROLES);

    List<LeadRow> filtered = filterLeads(from, to, status, search);
    log.info("[UCO LEAD] {} exports {} leads to Excel (from={}, to={}, status={})", exporter, filtered.size(), from, to, status);

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
      Sheet sh = wb.createSheet("Customer Leads");

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
      titleCell.setCellValue("UCO BANK - UCO REALTY HOME LOAN LEADS (from the bank Home Loan application page)");
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
        "Lead ID", "Customer Name", "Mobile", "Email", "State", "District",
        "Facility", "Loan Amount", "Lead Status", "Submitted On", "Notes"
      };
      final int notesCol = cols.length - 1;

      for (int i = 0; i < cols.length; i++) {
        Cell c = h.createCell(i);
        c.setCellValue(cols[i]);
        if (i == notesCol) {
          c.setCellStyle(actionHeaderStyle);
        } else {
          c.setCellStyle(headerStyle);
        }
      }

      // Data Rows
      int rowIdx = 4;
      for (LeadRow l : filtered) {
        Row r = sh.createRow(rowIdx);
        CellStyle rowStyle = (rowIdx % 2 == 0) ? altDataStyle : dataStyle;
        String[] values = {
          l.leadId, l.name, l.mobile, l.email, l.state, l.district,
          l.facility, l.getLoanAmount(), l.status, l.getFormattedDate(), ""
        };
        for (int i = 0; i < values.length; i++) {
          Cell c = r.createCell(i);
          c.setCellValue(values[i] != null ? values[i] : "");
          c.setCellStyle(i == 8 ? badgeStyle : rowStyle);   // column 8 = Lead Status
        }
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

  List<LeadRow> filterLeads(String from, String to, String status, String search) {
    return leads.find(parseDate(from), parseDate(to), status, search);   // the database does the filtering, newest first
  }

  /** One page of leads (the database skips and limits the rows). */
  List<LeadRow> leadsPage(String from, String to, String status, String search, int offset, int limit) {
    return leads.find(parseDate(from), parseDate(to), status, search, offset, limit);
  }

  int countLeads(String from, String to, String status, String search) {
    return leads.count(parseDate(from), parseDate(to), status, search);
  }

  private static LocalDate parseDate(String text) {
    if (text == null || text.trim().isEmpty()) return null;
    try {
      return LocalDate.parse(text);
    } catch (java.time.format.DateTimeParseException e) {
      throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST, "Dates must be in yyyy-MM-dd format");
    }
  }
}
