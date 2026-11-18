package com.ucorealty.controller;

import com.ucorealty.dto.LeadFilter;
import com.ucorealty.model.Lead;
import com.ucorealty.repository.LgEntryRepository;
import com.ucorealty.service.AuditService;
import com.ucorealty.util.PiiMask;
import com.ucorealty.util.StaffRoles;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The customer Leads page and its Excel export, for Head Office Marketing, Retail Banking and DIT. Filters travel by POST into the session;
 * customer name, mobile and e-mail are masked (setting uco.leads.mask-pii).
 * The leads are NOT stored by us: the bank's Home Loan page keeps them in HRMS table LG_ENTRY_REVAMP (CHANNEL = 'REALTY'),
 * and LgEntryRepository reads them from there. The page itself is leads.jsp.
 * Change this file when: the Leads page or the Excel columns change (see docs/MAINTENANCE_GUIDE.md, "Add or remove a column").
 */
@Controller
public class LeadsController {
  private static final int LEADS_PER_PAGE = 20;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LeadsController.class);
  private static final String FILTER_KEY = "LEAD_FILTER";
  private final LgEntryRepository leadSource;
  private final AuditService auditService;
  private final boolean maskPii;

  public LeadsController(LgEntryRepository leads, AuditService auditService,
                         @Value("${uco.leads.mask-pii:true}") boolean maskPii) {
    this.leadSource = leads;
    this.auditService = auditService;
    this.maskPii = maskPii;
  }

  /** The Leads page. The filter comes from the session, not from the address: only the page number is in the URL. */
  @GetMapping("/admin/leads")
  public String listLeads(@RequestParam(defaultValue = "1") int pageNo,
                          Authentication auth, HttpSession session, Model model) {
    // The nav link is hidden for other roles, but hiding is not protection: customer phone numbers need a real check
    StaffRoles.require(auth, StaffRoles.LEAD_ROLES);
    LeadFilter f = filterOf(session);
    int total = countLeads(f.getFrom(), f.getTo(), f.getStatus(), f.getSearch());
    int pages = Math.max(1, (total + LEADS_PER_PAGE - 1) / LEADS_PER_PAGE);
    int current = Math.min(Math.max(1, pageNo), pages);
    // only the 20 rows of this page are fetched from the bank table
    List<Lead> leads = leadsPage(f.getFrom(), f.getTo(), f.getStatus(), f.getSearch(), (current - 1) * LEADS_PER_PAGE, LEADS_PER_PAGE);
    maskIfRequired(leads);
    model.addAttribute("leadCount", total);
    model.addAttribute("pageCount", pages);
    model.addAttribute("pageNo", current);
    model.addAttribute("filter", f);
    model.addAttribute("maskPii", maskPii);
    model.addAttribute("leads", leads);
    model.addAttribute("statuses", leadSource.statuses());   // status codes for the filter drop-down
    return "leads";
  }

  /** Saves the filter in the session (a POST: the search can be a name or a phone number) and shows the first page. */
  @PostMapping("/admin/leads/filter")
  public String applyFilter(@RequestParam(required = false) String from,
                            @RequestParam(required = false) String to,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String clear,
                            Authentication auth, HttpSession session) {
    StaffRoles.require(auth, StaffRoles.LEAD_ROLES);
    if (clear != null) {
      session.removeAttribute(FILTER_KEY);
    } else {
      parseDate(from);   // a wrong date is refused here (400), not when the page is shown
      parseDate(to);
      session.setAttribute(FILTER_KEY, new LeadFilter(from, to, status, search));
    }
    return "redirect:/admin/leads";
  }

  /** The Excel file of the leads the page is filtered to (a POST with the security token: it hands out customer data and writes an audit row). */
  @PostMapping("/admin/leads/export")
  public void export(HttpServletResponse res, HttpSession session, Authentication auth) throws IOException {
    StaffRoles.require(auth, StaffRoles.LEAD_ROLES);   // before the audit row: a refused export must not be recorded as an export
    auditService.logActivity(auth.getName(), "EXPORT_LEADS", "LEAD", "EXCEL", "Exported leads to Excel");
    LeadFilter f = filterOf(session);
    writeExcel(f.getFrom(), f.getTo(), f.getStatus(), f.getSearch(), res, auth);
  }

  private static LeadFilter filterOf(HttpSession session) {
    Object f = session.getAttribute(FILTER_KEY);
    return f instanceof LeadFilter ? (LeadFilter) f : LeadFilter.empty();
  }

  /** Customer name, mobile and e-mail are shown masked unless the setting uco.leads.mask-pii is false (the bank decides). */
  private void maskIfRequired(List<Lead> leads) {
    if (!maskPii) return;
    for (Lead l : leads) {   // the rows are detached copies read from the bank table: changing them changes nothing in the database
      l.name = PiiMask.name(l.name);
      l.mobile = PiiMask.mobile(l.mobile);
      l.email = PiiMask.email(l.email);
    }
  }

  /** Writes the filtered leads as a styled Excel (.xlsx) file with Call Center action columns. */
  private void writeExcel(String from, String to, String status, String search,
                          HttpServletResponse res, Authentication auth) throws IOException {
    String exporter = StaffRoles.require(auth, StaffRoles.LEAD_ROLES);

    List<Lead> filtered = filterLeads(from, to, status, search);
    maskIfRequired(filtered);
    log.info("[UCO LEAD] {} exports {} leads to Excel (from={}, to={}, status={})", exporter, filtered.size(), from, to, status);

    // File name: uco-realty-leads_ plus the date of the download, e.g. uco-realty-leads_2026-11-18.xlsx
    String filename = "uco-realty-leads_" + LocalDate.now() + ".xlsx";

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
                          (maskPii ? " | Customer name, mobile and e-mail are masked" : "") +
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
      for (Lead l : filtered) {
        Row r = sh.createRow(rowIdx);
        CellStyle rowStyle = (rowIdx % 2 == 0) ? altDataStyle : dataStyle;
        String[] values = {
          l.leadId, l.name, l.mobile, l.email, l.state, l.district,
          l.facility, l.getLoanAmount(), l.status, l.getFormattedDate(), ""
        };
        for (int i = 0; i < values.length; i++) {
          Cell c = r.createCell(i);
          c.setCellValue(safeText(values[i]));
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

  /**
   * Text for an Excel cell. A customer typed these values on a public page, so text that Excel could read as a formula
   * (it starts with = or @, a tab or a line break, or with + or - followed by letters) gets a ' in front. Excel then
   * shows it as plain text and never runs it. Phone numbers such as +91 98765 43210 are left as they are.
   */
  static String safeText(String text) {
    if (text == null) return "";
    if (text.isEmpty()) return text;
    char first = text.charAt(0);
    boolean formulaStart = first == '=' || first == '@' || first == '\t' || first == '\r' || first == '\n';
    boolean signedText = (first == '+' || first == '-') && !text.matches("[-+0-9 ()./]*");
    return (formulaStart || signedText) ? "'" + text : text;
  }

  private List<Lead> filterLeads(String from, String to, String status, String search) {
    return leadSource.find(parseDate(from), parseDate(to), status, search);   // the database does the filtering, newest first
  }

  /** One page of leads (the database skips and limits the rows). */
  private List<Lead> leadsPage(String from, String to, String status, String search, int offset, int limit) {
    return leadSource.find(parseDate(from), parseDate(to), status, search, offset, limit);
  }

  private int countLeads(String from, String to, String status, String search) {
    return leadSource.count(parseDate(from), parseDate(to), status, search);
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
