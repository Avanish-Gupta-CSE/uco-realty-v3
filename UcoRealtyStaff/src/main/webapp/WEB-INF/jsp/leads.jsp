<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<c:set var="pageTitle" value="Customer Leads - UCO Realty" scope="request"/>
<c:set var="activeTab" value="leads" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container-xxl py-4" id="main-customer-leads">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2" id="leads-header-row">
        <div>
            <h2 class="h4 text-primary fw-bold mb-1" id="leads-title">Home Loan Leads from UCO Realty</h2>
            <p class="text-muted small mb-0" id="leads-subtitle">
                Customers who clicked Apply for Home Loan on the realty portal and filled the bank's Home Loan application (read from the bank's lead table).<c:if test="${maskPii}"> Customer name, mobile and e-mail are shown masked, as the bank's security policy requires.</c:if>
            </p>
        </div>

        <div>
            <form action="${pageContext.request.contextPath}/admin/leads/export" method="post" class="d-inline m-0" id="form-export-leads">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <button type="submit" class="btn btn-success" id="btn-export-leads">&darr; Export Leads to Excel (.xlsx)</button>
            </form>
        </div>
    </div>

    <%-- Filter Bar --%>
    <div class="filter-bar mb-4" id="leads-filter-container">
        <form action="${pageContext.request.contextPath}/admin/leads/filter" method="post" class="row g-3 align-items-end" id="form-leads-filter">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="col-md-2">
                <label for="from" class="form-label fw-semibold small">From Date</label>
                <input type="date" id="from" name="from" class="form-control form-control-sm" value="<c:out value="${filter.from}"/>">
            </div>

            <div class="col-md-2">
                <label for="to" class="form-label fw-semibold small">To Date</label>
                <input type="date" id="to" name="to" class="form-control form-control-sm" value="<c:out value="${filter.to}"/>">
            </div>

            <div class="col-md-3">
                <label for="search" class="form-label fw-semibold small">Search Name / Mobile / Lead ID</label>
                <input type="text" id="search" name="search" class="form-control form-control-sm" value="<c:out value="${filter.search}"/>" placeholder="Name, mobile or lead ID">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm flex-fill" id="btn-leads-filter-apply">Filter</button>
                <button type="submit" name="clear" value="1" formnovalidate class="btn btn-secondary btn-sm flex-fill" id="btn-leads-filter-reset">Reset</button>
            </div>
        </form>
    </div>

    <%-- Leads Table --%>
    <div class="card-custom p-0 overflow-hidden mb-4" id="card-leads-table">
        <div class="card-title-custom p-3 mb-0 bg-light border-bottom" id="leads-table-title">
            Newest first &middot; ${leadCount} ${leadCount == 1 ? 'lead' : 'leads'}
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 table-cards" id="table-leads">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">Lead ID</th>
                        <th>Customer Name</th>
                        <th>Mobile</th>
                        <th>Email</th>
                        <th>Location</th>
                        <th>Facility</th>
                        <th>Loan Amount</th>
                        <th class="pe-3">Submitted On</th>
                    </tr>
                </thead>
                <tbody id="leads-table-body">
                    <c:choose>
                        <c:when test="${empty leads}">
                            <tr id="row-no-leads">
                                <td colspan="8" class="text-center py-4 text-muted">
                                    No customer leads found matching your criteria.
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="l" items="${leads}" varStatus="vs">
                                <tr id="row-lead-${vs.index}">
                                    <td class="ps-3" data-label="Lead"><span class="badge bg-light text-secondary border" id="lead-badge-id-${vs.index}"><c:out value="${l.leadId}"/></span></td>
                                    <td data-label="Customer"><strong id="lead-name-${vs.index}"><c:out value="${l.name}"/></strong></td>
                                    <td data-label="Mobile"><c:choose><c:when test="${maskPii}"><span class="fw-semibold" id="lead-phone-${vs.index}"><c:out value="${l.mobile}"/></span></c:when><c:otherwise><a href="tel:<c:out value="${l.mobile}"/>" class="fw-semibold text-primary" id="lead-phone-${vs.index}"><c:out value="${l.mobile}"/></a></c:otherwise></c:choose></td>
                                    <td data-label="Email"><small id="lead-email-${vs.index}"><c:out value="${l.email}"/></small></td>
                                    <td data-label="Location"><c:out value="${l.location}"/></td>
                                    <td data-label="Facility"><c:out value="${l.facility}"/></td>
                                    <td data-label="Loan Amount" class="text-nowrap"><c:out value="${l.loanAmount}"/></td>
                                    <td class="pe-3 text-nowrap" data-label="Date"><small class="text-muted" id="lead-date-${vs.index}"><c:out value="${l.formattedDate}"/></small></td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
        <c:if test="${pageCount > 1}">
            <div class="card-footer bg-white d-flex justify-content-center py-3" id="footer-leads-pages">
                <ul class="pagination pagination-sm pagination-custom mb-0" id="ul-leads-pages">
                    <c:forEach var="n" begin="1" end="${pageCount}">
                        <c:url var="leadPageUrl" value="/admin/leads"><c:param name="pageNo" value="${n}"/></c:url>
                        <li class="page-item ${pageNo == n ? 'active' : ''}"><a class="page-link" href="${leadPageUrl}" id="link-leads-page-${n}">${n}</a></li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>
    </div>
</main>

<jsp:include page="common/footer.jsp"/>
