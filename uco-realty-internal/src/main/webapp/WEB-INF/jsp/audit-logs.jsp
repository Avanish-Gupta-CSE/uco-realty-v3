<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>

<c:set var="pageTitle" value="Logs - UCO Realty" scope="request"/>
<c:set var="activeTab" value="audit" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container-fluid px-lg-4 px-3 py-4" id="main-audit-logs">
    <!-- Breadcrumb & Header Row -->
    <div class="mb-4" id="audit-header-row">
        <nav aria-label="breadcrumb" id="audit-breadcrumb-nav">
            <ol class="breadcrumb mb-2" id="audit-breadcrumb-list">
                <li class="breadcrumb-item" id="breadcrumb-home"><a href="${pageContext.request.contextPath}/admin/projects" id="link-breadcrumb-home">Project Management</a></li>
                <li class="breadcrumb-item active" aria-current="page" id="breadcrumb-current">Logs</li>
            </ol>
        </nav>
        <div class="d-flex justify-content-between align-items-center" id="audit-title-wrapper">
            <div>
                <h2 class="h4 text-primary fw-bold mb-1" id="audit-heading">System Logs &amp; Activity Records</h2>
                <p class="text-muted small mb-0" id="audit-subheading">
                    Strictly restricted to Superadmin (DIT) &bull; Comprehensive log trail of staff operations and login security history.
                </p>
            </div>
        </div>
    </div>

    <!-- Dual Tab Navigation -->
    <ul class="nav nav-tabs mb-4" id="audit-tabs" role="tablist">
        <li class="nav-item" role="presentation" id="tab-item-activity">
            <button class="nav-link ${activeSubTab != 'login' ? 'active' : ''} fw-semibold"
                    id="tab-btn-activity"
                    data-bs-toggle="tab"
                    data-bs-target="#tab-pane-activity"
                    type="button"
                    role="tab"
                    aria-controls="tab-pane-activity"
                    aria-selected="${activeSubTab != 'login' ? 'true' : 'false'}">
                User &amp; Workflow Activities
                <c:if test="${not empty activityLogs}">
                    <span class="badge bg-secondary ms-1" id="badge-activity-count">${activityLogs.totalElements}</span>
                </c:if>
            </button>
        </li>
        <li class="nav-item" role="presentation" id="tab-item-login">
            <button class="nav-link ${activeSubTab == 'login' ? 'active' : ''} fw-semibold"
                    id="tab-btn-login"
                    data-bs-toggle="tab"
                    data-bs-target="#tab-pane-login"
                    type="button"
                    role="tab"
                    aria-controls="tab-pane-login"
                    aria-selected="${activeSubTab == 'login' ? 'true' : 'false'}">
                Login Security History
                <c:if test="${not empty loginAudits}">
                    <span class="badge bg-secondary ms-1" id="badge-login-count">${loginAudits.totalElements}</span>
                </c:if>
            </button>
        </li>
    </ul>

    <!-- Tab Contents -->
    <div class="tab-content" id="audit-tab-content">

        <!-- Tab Pane 1: User & Workflow Activities -->
        <div class="tab-pane fade ${activeSubTab != 'login' ? 'show active' : ''}"
             id="tab-pane-activity"
             role="tabpanel"
             aria-labelledby="tab-btn-activity">
            <div class="card border-0 shadow-sm" id="card-activity-logs">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center" id="card-header-activity">
                    <h3 class="h6 fw-bold mb-0 text-primary" id="title-activity-card">Workflow &amp; System Events</h3>
                    <small class="text-muted" id="text-activity-stats">
                        Showing ${activityLogs.numberOfElements} of ${activityLogs.totalElements} events
                    </small>
                </div>
                <div class="card-body p-0" id="card-body-activity">
                    <div class="table-responsive" id="table-activity-responsive">
                        <table class="table table-hover align-middle mb-0 small" id="table-activity-logs">
                            <thead class="table-light" id="thead-activity">
                                <tr>
                                    <th class="ps-3 table-col-time" id="th-act-time">Timestamp</th>
                                    <th class="table-col-user" id="th-act-user">Operator</th>
                                    <th class="table-col-module" id="th-act-module">Module</th>
                                    <th class="table-col-module" id="th-act-entity">Entity ID</th>
                                    <th class="table-col-action" id="th-act-action">Action</th>
                                    <th class="table-col-details" id="th-act-details">Operational Details</th>
                                </tr>
                            </thead>
                            <tbody id="tbody-activity">
                                <c:choose>
                                    <c:when test="${not empty activityLogs.content}">
                                        <c:forEach var="log" items="${activityLogs.content}" varStatus="status">
                                            <tr id="row-activity-${status.index}">
                                                <td class="ps-3 text-muted text-nowrap" id="cell-act-time-${status.index}">
                                                    ${log.formattedCreatedAt}
                                                </td>
                                                <td class="fw-bold text-primary" id="cell-act-user-${status.index}">
                                                    ${log.byUser}
                                                </td>
                                                <td id="cell-act-module-${status.index}">
                                                    <span class="badge bg-secondary" id="badge-act-module-${status.index}">${log.entityType}</span>
                                                </td>
                                                <td id="cell-act-entity-${status.index}">
                                                    <code>${log.entityId}</code>
                                                </td>
                                                <td id="cell-act-action-${status.index}">
                                                    <c:choose>
                                                        <c:when test="${log.action == 'APPROVE' or log.action == 'PUBLISH'}">
                                                            <span class="badge bg-success" id="badge-act-action-${status.index}">${log.action}</span>
                                                        </c:when>
                                                        <c:when test="${log.action == 'REJECT' or log.action == 'DELETE_USER'}">
                                                            <span class="badge bg-danger" id="badge-act-action-${status.index}">${log.action}</span>
                                                        </c:when>
                                                        <c:when test="${log.action == 'CREATE' or log.action == 'CREATE_USER' or log.action == 'SUBMIT'}">
                                                            <span class="badge bg-primary" id="badge-act-action-${status.index}">${log.action}</span>
                                                        </c:when>
                                                        <c:when test="${log.action == 'UPDATE' or log.action == 'RESET_PASSWORD' or log.action == 'CHANGE_PASSWORD'}">
                                                            <span class="badge bg-warning text-dark" id="badge-act-action-${status.index}">${log.action}</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-info text-dark" id="badge-act-action-${status.index}">${log.action}</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td id="cell-act-details-${status.index}">
                                                    <span class="audit-details-code" id="code-act-details-${status.index}">${log.details}</span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr id="row-activity-empty">
                                            <td colspan="6" class="text-center py-4 text-muted" id="cell-activity-empty">
                                                No activity logs recorded yet.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Activity Pagination Footer -->
                <c:if test="${activityLogs.totalPages > 1}">
                    <div class="card-footer bg-white border-top d-flex justify-content-between align-items-center py-3" id="footer-activity-pagination">
                        <small class="text-muted" id="text-activity-page-info">
                            Showing page <strong>${activityLogs.number + 1}</strong> of <strong>${activityLogs.totalPages}</strong>
                        </small>
                        <nav aria-label="Activity page navigation" id="nav-activity-pagination">
                            <ul class="pagination pagination-sm pagination-custom mb-0" id="ul-activity-pagination">
                                <li class="page-item <c:if test='${not activityLogs.hasPrevious()}'>disabled</c:if>">
                                    <a class="page-link" href="?pageActivity=${activityLogs.number - 1}&pageLogin=${pageLogin}&tab=activity" id="link-act-prev">&laquo; Prev</a>
                                </li>
                                <c:forEach var="i" begin="0" end="${activityLogs.totalPages - 1}">
                                    <li class="page-item <c:if test='${activityLogs.number == i}'>active</c:if>">
                                        <a class="page-link" href="?pageActivity=${i}&pageLogin=${pageLogin}&tab=activity" id="link-act-page-${i}">${i + 1}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${not activityLogs.hasNext()}'>disabled</c:if>">
                                    <a class="page-link" href="?pageActivity=${activityLogs.number + 1}&pageLogin=${pageLogin}&tab=activity" id="link-act-next">Next &raquo;</a>
                                </li>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>

        <!-- Tab Pane 2: Login Security History -->
        <div class="tab-pane fade ${activeSubTab == 'login' ? 'show active' : ''}"
             id="tab-pane-login"
             role="tabpanel"
             aria-labelledby="tab-btn-login">
            <div class="card border-0 shadow-sm" id="card-login-logs">
                <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center" id="card-header-login">
                    <h3 class="h6 fw-bold mb-0 text-primary" id="title-login-card">Authentication &amp; Access Log</h3>
                    <small class="text-muted" id="text-login-stats">
                        Showing ${loginAudits.numberOfElements} of ${loginAudits.totalElements} login attempts
                    </small>
                </div>
                <div class="card-body p-0" id="card-body-login">
                    <div class="table-responsive" id="table-login-responsive">
                        <table class="table table-hover align-middle mb-0 small" id="table-login-logs">
                            <thead class="table-light" id="thead-login">
                                <tr>
                                    <th class="ps-3 table-col-time" id="th-log-time">Login Timestamp</th>
                                    <th class="table-col-user" id="th-log-user">Username</th>
                                    <th class="table-col-ip" id="th-log-ip">Client IP Address</th>
                                    <th class="table-col-status" id="th-log-status">Result</th>
                                    <th class="table-col-details" id="th-log-remarks">Security Remarks</th>
                                </tr>
                            </thead>
                            <tbody id="tbody-login">
                                <c:choose>
                                    <c:when test="${not empty loginAudits.content}">
                                        <c:forEach var="audit" items="${loginAudits.content}" varStatus="status">
                                            <tr id="row-login-${status.index}">
                                                <td class="ps-3 text-muted text-nowrap" id="cell-log-time-${status.index}">
                                                    ${audit.formattedLoginTime}
                                                </td>
                                                <td class="fw-bold" id="cell-log-user-${status.index}">
                                                    ${audit.username}
                                                </td>
                                                <td id="cell-log-ip-${status.index}">
                                                    <code>${audit.ipAddress}</code>
                                                </td>
                                                <td id="cell-log-status-${status.index}">
                                                    <c:choose>
                                                        <c:when test="${audit.success}">
                                                            <span class="badge bg-success" id="badge-login-status-${status.index}">
                                                                SUCCESS
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-danger" id="badge-login-status-${status.index}">
                                                                FAILED
                                                            </span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="${not audit.success ? 'text-danger fw-semibold' : 'text-muted'}" id="cell-log-remarks-${status.index}">
                                                    ${audit.remarks}
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr id="row-login-empty">
                                            <td colspan="5" class="text-center py-4 text-muted" id="cell-login-empty">
                                                No login security audit records found.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Login Pagination Footer -->
                <c:if test="${loginAudits.totalPages > 1}">
                    <div class="card-footer bg-white border-top d-flex justify-content-between align-items-center py-3" id="footer-login-pagination">
                        <small class="text-muted" id="text-login-page-info">
                            Showing page <strong>${loginAudits.number + 1}</strong> of <strong>${loginAudits.totalPages}</strong>
                        </small>
                        <nav aria-label="Login page navigation" id="nav-login-pagination">
                            <ul class="pagination pagination-sm pagination-custom mb-0" id="ul-login-pagination">
                                <li class="page-item <c:if test='${not loginAudits.hasPrevious()}'>disabled</c:if>">
                                    <a class="page-link" href="?pageActivity=${pageActivity}&pageLogin=${loginAudits.number - 1}&tab=login" id="link-log-prev">&laquo; Prev</a>
                                </li>
                                <c:forEach var="i" begin="0" end="${loginAudits.totalPages - 1}">
                                    <li class="page-item <c:if test='${loginAudits.number == i}'>active</c:if>">
                                        <a class="page-link" href="?pageActivity=${pageActivity}&pageLogin=${i}&tab=login" id="link-log-page-${i}">${i + 1}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item <c:if test='${not loginAudits.hasNext()}'>disabled</c:if>">
                                    <a class="page-link" href="?pageActivity=${pageActivity}&pageLogin=${loginAudits.number + 1}&tab=login" id="link-log-next">Next &raquo;</a>
                                </li>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>

    </div>
</main>

<jsp:include page="common/footer.jsp"/>
<script id="audit-logs-page-js" src="${pageContext.request.contextPath}/assets/js/audit-logs.js"></script>
</body>
</html>

