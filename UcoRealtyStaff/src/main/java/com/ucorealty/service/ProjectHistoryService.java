package com.ucorealty.service;

import com.ucorealty.model.AuditLog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the "Workflow and Audit History" timeline of one project from the activity log: who did what, when, with which remarks.
 * Only the approval steps are shown: a maker's private drafting (draft created, saved or discarded) is not part of the history. Change this file when: a new step should appear in the timeline.
 */
@Service
public class ProjectHistoryService {
  /** Audit action -> {label shown, badge colour class}. */
  private static final Map<String, String[]> STEPS = new LinkedHashMap<>();
  static {
    STEPS.put("SUBMIT_FOR_APPROVAL", new String[] {"Submitted", "primary"});
    STEPS.put("CHANGE_SUBMITTED", new String[] {"Change submitted", "primary"});
    STEPS.put("DELETE_REQUESTED", new String[] {"Deletion requested", "primary"});
    STEPS.put("CHECKER_NEEDS_CORRECTION", new String[] {"Returned for correction", "warning"});
    STEPS.put("CHANGE_RETURNED", new String[] {"Returned for correction", "warning"});
    STEPS.put("CHECKER_APPROVE", new String[] {"Approved", "success"});
    STEPS.put("CHANGE_APPROVED", new String[] {"Change approved", "success"});
    STEPS.put("DELETE_APPROVED", new String[] {"Deletion approved", "success"});
    STEPS.put("CHECKER_REJECT", new String[] {"Rejected", "danger"});
    STEPS.put("CHANGE_REJECTED", new String[] {"Change rejected", "danger"});
    STEPS.put("DELETE_REJECTED", new String[] {"Deletion rejected", "danger"});
    STEPS.put("REQUEST_WITHDRAWN", new String[] {"Withdrawn by maker", "secondary"});
    STEPS.put("CHANGE_WITHDRAWN", new String[] {"Withdrawn by maker", "secondary"});   // older rows
  }

  private final AuditService audit;
  private final HrmsService hrms;

  public ProjectHistoryService(AuditService audit, HrmsService hrms) {
    this.audit = audit;
    this.hrms = hrms;
  }

  /** One timeline row, as the page reads it. */
  public static class Step {
    private final String label, badge, time, empId, name, remarks;
    Step(String label, String badge, String time, String empId, String name, String remarks) {
      this.label = label; this.badge = badge; this.time = time; this.empId = empId; this.name = name; this.remarks = remarks;
    }
    public String getLabel() { return label; }
    public String getBadge() { return badge; }
    public String getTime() { return time; }
    public String getEmpId() { return empId; }
    public String getName() { return name; }
    public String getRemarks() { return remarks; }
  }

  /** The timeline of a project, oldest step first. */
  public List<Step> of(String projectId) {
    List<AuditLog> rows = audit.projectSteps(projectId, STEPS.keySet());
    List<String> people = new ArrayList<>();
    for (AuditLog r : rows) if (r.byUser != null && !people.contains(r.byUser)) people.add(r.byUser);
    Map<String, String> names = hrms.namesOf((Collection<String>) people);
    List<Step> out = new ArrayList<>();
    for (AuditLog r : rows) {
      String[] st = STEPS.get(r.action);
      String name = names.get(r.byUser);
      String time = r.createdAt == null ? "-" : r.createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
      String remarks = r.newVal != null && !r.newVal.isBlank() ? r.newVal : (r.oldVal != null ? r.oldVal : "");
      if (r.action.endsWith("APPROVE") || r.action.endsWith("APPROVED")) {
        // an approval's remark is only what the checker wrote (kept in new_val; the summary is in old_val). Older rows have no old_val and keep nothing.
        remarks = r.oldVal != null && r.newVal != null ? r.newVal : "";
      }
      out.add(new Step(st[0], st[1], time, r.byUser, name != null ? name : r.byUser, remarks));
    }
    return out;
  }
}
