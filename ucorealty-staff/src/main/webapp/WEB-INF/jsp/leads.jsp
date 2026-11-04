<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<c:set var="pageTitle" value="Customer Leads - UCO Realty" scope="request"/>
<c:set var="activeTab" value="leads" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container-fluid px-lg-4 px-3 py-4" id="main-customer-leads">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2" id="leads-header-row">
        <div>
            <h2 class="h4 text-primary fw-bold mb-1" id="leads-title">Customer Leads &amp; Home Loan Inquiries</h2>
            <p class="text-muted small mb-0" id="leads-subtitle">
                Customer loan applications and interest captured through the realty portal.
            </p>
        </div>

        <div>
            <a href="${pageContext.request.contextPath}/admin/leads/export.xlsx?from=${param.from}&to=${param.to}&status=${param.status}&search=${param.search}" class="btn btn-success" id="btn-export-leads">
                &darr; Export Leads to Excel (.xlsx)
            </a>
        </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar mb-4" id="leads-filter-container">
        <form action="${pageContext.request.contextPath}/admin/leads" method="get" class="row g-3 align-items-end" id="form-leads-filter">
            <div class="col-md-2">
                <label for="from" class="form-label fw-semibold small">From Date</label>
                <input type="date" id="from" name="from" class="form-control form-control-sm" value="${param.from}">
            </div>

            <div class="col-md-2">
                <label for="to" class="form-label fw-semibold small">To Date</label>
                <input type="date" id="to" name="to" class="form-control form-control-sm" value="${param.to}">
            </div>

            <div class="col-md-3">
                <label for="status" class="form-label fw-semibold small">Lead Status</label>
                <select id="status" name="status" class="form-select form-select-sm">
                    <option value="">-- All Statuses --</option>
                    <option value="Visitor" ${param.status == 'Visitor' || param.status == 'VISITOR' ? 'selected' : ''}>Visitor</option>
                    <option value="Showed interest in Home Loan" ${param.status == 'Showed interest in Home Loan' || param.status == 'SUCCESSFUL_APPLIED' ? 'selected' : ''}>Showed interest in Home Loan</option>
                </select>
            </div>

            <div class="col-md-3">
                <label for="search" class="form-label fw-semibold small">Search Customer / Phone</label>
                <input type="text" id="search" name="search" class="form-control form-control-sm" value="${param.search}" placeholder="Customer name or mobile">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm flex-fill" id="btn-leads-filter-apply">Filter</button>
                <a href="${pageContext.request.contextPath}/admin/leads" class="btn btn-secondary btn-sm flex-fill" id="btn-leads-filter-reset">Reset</a>
            </div>
        </form>
    </div>

    <!-- Leads Table -->
    <div class="card-custom p-0 overflow-hidden mb-4" id="card-leads-table">
        <div class="card-title-custom p-3 mb-0 bg-light border-bottom" id="leads-table-title">
            Recent Customer Leads
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0" id="table-leads">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Lead ID</th>
                        <th>Customer Name</th>
                        <th>Mobile Number</th>
                        <th>Associated Project</th>
                        <th>Lead Status</th>
                        <th class="pe-3">Captured Date</th>
                    </tr>
                </thead>
                <tbody id="leads-table-body">
                    <c:choose>
                        <c:when test="${empty leads}">
                            <tr id="row-no-leads">
                                <td colspan="6" class="text-center py-4 text-muted">
                                    No customer leads found matching your criteria.
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="l" items="${leads}">
                                <tr id="row-lead-${l.id}">
                                    <td class="ps-3"><span class="badge bg-light text-secondary border" id="lead-badge-id-${l.id}">#<c:out value="${l.id}"/></span></td>
                                    <td><strong id="lead-name-${l.id}"><c:out value="${l.customerName}"/></strong></td>
                                    <td><a href="tel:<c:out value="${l.phone}"/>" class="fw-semibold text-primary" id="lead-phone-${l.id}"><c:out value="${l.phone}"/></a></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${projectNames[l.projectId] != null}">
                                                <a href="${pageContext.request.contextPath}/project/<c:out value="${l.projectId}"/>" class="text-primary fw-semibold" id="lead-project-link-${l.id}">
                                                    <c:out value="${projectNames[l.projectId]}"/>
                                                </a>
                                            </c:when>
                                            <c:when test="${l.projectId != null}">
                                                <span class="font-monospace small" id="lead-project-id-${l.id}"><c:out value="${l.projectId}"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted small" id="lead-general-inquiry-${l.id}">General Inquiry</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${l.status == 'Showed interest in Home Loan' || l.status == 'SUCCESSFUL_APPLIED'}">
                                                <span class="badge badge-approved" id="lead-status-badge-${l.id}">Showed interest in Home Loan</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-draft" id="lead-status-badge-${l.id}">Visitor</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="pe-3 text-nowrap"><small class="text-muted" id="lead-date-${l.id}">${l.formattedDate}</small></td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </div>
</main>

<jsp:include page="common/footer.jsp"/>
