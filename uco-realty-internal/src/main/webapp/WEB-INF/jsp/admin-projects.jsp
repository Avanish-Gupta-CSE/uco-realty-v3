<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>

<c:set var="pageTitle" value="Project Management - UCO Realty" scope="request"/>
<c:set var="activeTab" value="projects" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container-fluid px-lg-4 px-3 py-4" id="main-admin-projects">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2" id="admin-header-row">
        <div>
            <h2 class="h4 text-primary fw-bold mb-1" id="admin-title">Approved Housing Project Directory</h2>
            <p class="text-muted small mb-0" id="admin-subtitle">
                Maker-Checker workflow for zonal and corporate office housing approvals.
            </p>
        </div>

        <div>
            <sec:authorize access="hasAnyRole('ZO_MAKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                <a href="${pageContext.request.contextPath}/admin/projects/new" class="btn btn-primary" id="btn-add-new-project">
                    + Add New Project
                </a>
            </sec:authorize>
        </div>
    </div>

    <!-- Alert messages -->
    <c:if test="${message != null}">
        <div class="alert alert-success py-2 small mb-3" id="admin-alert-message">${message}</div>
    </c:if>
    <c:if test="${error != null}">
        <div class="alert alert-danger py-2 small mb-3" id="admin-alert-error">${error}</div>
    </c:if>

    <!-- Search & Filter Controls -->
    <div class="filter-bar mb-4" id="card-projects-filter">
        <form action="${pageContext.request.contextPath}/admin/projects" method="get" class="row g-3 align-items-end" id="form-admin-projects-filter">
            <div class="col-lg-5 col-md-6">
                <label for="search-projects" class="form-label fw-semibold small">Search Projects</label>
                <input type="text" id="search-projects" name="search" class="form-control form-control-sm"
                       value="${currentSearch}" placeholder="Project Name, ID, Builder, RERA, City, Pincode...">
            </div>

            <div class="col-lg-3 col-md-3">
                <label for="select-status" class="form-label fw-semibold small">Project Status</label>
                <select id="select-status" name="status" class="form-select form-select-sm">
                    <option value="">-- All Statuses --</option>
                    <option value="DRAFT" ${currentStatus == 'DRAFT' ? 'selected' : ''}>Draft</option>
                    <option value="PENDING_APPROVAL" ${currentStatus == 'PENDING_APPROVAL' ? 'selected' : ''}>Pending Approval</option>
                    <option value="APPROVED" ${currentStatus == 'APPROVED' ? 'selected' : ''}>Approved</option>
                    <option value="NEEDS_CORRECTION" ${currentStatus == 'NEEDS_CORRECTION' ? 'selected' : ''}>Correction Required</option>
                    <option value="REJECTED" ${currentStatus == 'REJECTED' ? 'selected' : ''}>Rejected</option>
                </select>
            </div>

            <sec:authorize access="hasAnyRole('SUPERADMIN_DIT', 'HO_ADMIN', 'HO_MARKETING', 'HO_RETAIL', 'CALL_CENTRE')">
                <div class="col-lg-2 col-md-3">
                    <label for="select-zone" class="form-label fw-semibold small">Zonal Office</label>
                    <select id="select-zone" name="zone" class="form-select form-select-sm">
                        <option value="">-- All Zones --</option>
                        <c:forEach var="z" items="${zones}">
                            <option value="${z}" ${currentZone == z ? 'selected' : ''}>${z} Zone</option>
                        </c:forEach>
                    </select>
                </div>
            </sec:authorize>

            <div class="col-lg-2 col-md-12 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm flex-fill" id="btn-projects-filter-apply">Search</button>
                <a href="${pageContext.request.contextPath}/admin/projects" class="btn btn-secondary btn-sm flex-fill" id="btn-projects-filter-reset">Reset</a>
            </div>
        </form>

        <!-- Quick Status Filter Pills -->
        <div class="d-flex gap-2 flex-wrap align-items-center mt-3 pt-3 border-top small" id="status-filters-wrap">
            <span class="fw-semibold text-secondary me-2" id="text-filter-by-status">Quick Status:</span>
            <a href="?status=&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${empty currentStatus ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-all">All</a>
            <a href="?status=DRAFT&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'DRAFT' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-draft">Draft</a>
            <a href="?status=PENDING_APPROVAL&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'PENDING_APPROVAL' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-pending">Pending Approval</a>
            <a href="?status=APPROVED&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'APPROVED' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-approved">Approved</a>
            <a href="?status=NEEDS_CORRECTION&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'NEEDS_CORRECTION' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-correction">Correction Required</a>
            <a href="?status=REJECTED&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'REJECTED' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-rejected">Rejected</a>
        </div>
    </div>

    <!-- Table of Projects -->
    <div class="card-custom p-0 overflow-hidden mb-4" id="card-projects-table">
        <div class="card-header bg-white border-bottom d-flex justify-content-between align-items-center py-2" id="header-projects-summary">
            <span class="fw-semibold text-secondary small" id="text-projects-summary">
                Showing <strong class="text-dark">${projectPage.numberOfElements}</strong> of <strong class="text-dark">${projectPage.totalElements}</strong> housing projects
            </span>
            <c:if test="${not empty currentSearch || not empty currentStatus || not empty currentZone}">
                <span class="badge bg-light text-primary border" id="badge-active-filters">Filtered</span>
            </c:if>
        </div>

        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 table-admin-projects" id="table-admin-projects">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3 col-th-project">Project ID &amp; Name</th>
                        <th class="col-th-location">Zone &amp; Location</th>
                        <th class="col-th-builder">Builder</th>
                        <th class="col-th-inventory">Inventory</th>
                        <th class="col-th-status">Status</th>
                        <th class="col-th-creator">Created By</th>
                        <th class="text-end pe-3 col-th-actions">Actions</th>
                    </tr>
                </thead>
                <tbody id="projects-table-body">
                    <c:choose>
                        <c:when test="${empty projects}">
                            <tr id="row-no-projects">
                                <td colspan="7" class="text-center py-5 text-muted">
                                    <div class="py-3">
                                        <p class="mb-2 fw-semibold">No housing projects found matching the selected filters.</p>
                                        <a href="${pageContext.request.contextPath}/admin/projects" class="btn btn-outline-primary btn-sm" id="btn-empty-reset">
                                            Clear Filters
                                        </a>
                                    </div>
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="p" items="${projects}">
                                <tr id="row-project-${p.id}">
                                    <td class="ps-3 col-th-project">
                                        <div class="project-title mb-1" id="row-name-${p.id}">${p.projectName}</div>
                                        <span class="project-badge-id font-monospace" id="row-id-${p.id}">${p.id}</span>
                                    </td>
                                    <td class="col-th-location">
                                        <div><strong>${p.zoneName}</strong> Zone</div>
                                        <small class="text-muted">${p.cityName}, ${p.stateName}</small>
                                    </td>
                                    <td class="col-th-builder">${p.builderName != null ? p.builderName : '-'}</td>
                                    <td class="col-th-inventory">
                                        <div><strong>${p.unsoldInventory != null ? p.unsoldInventory : 0}</strong> avail.</div>
                                        <small class="text-muted">${p.totalInventory != null ? p.totalInventory : (p.totalUnits != null ? p.totalUnits : 0)} total units</small>
                                    </td>
                                    <td class="col-th-status">
                                        <c:choose>
                                            <c:when test="${p.status == 'APPROVED'}">
                                                <span class="badge badge-approved" id="badge-status-${p.id}">Approved</span>
                                            </c:when>
                                            <c:when test="${p.status == 'PENDING_APPROVAL'}">
                                                <span class="badge badge-pending" id="badge-status-${p.id}">Pending Approval</span>
                                            </c:when>
                                            <c:when test="${p.status == 'NEEDS_CORRECTION'}">
                                                <span class="badge badge-correction" id="badge-status-${p.id}">Needs Correction</span>
                                            </c:when>
                                            <c:when test="${p.status == 'REJECTED'}">
                                                <span class="badge badge-rejected" id="badge-status-${p.id}">Rejected</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-draft" id="badge-status-${p.id}">Draft</span>
                                            </c:otherwise>
                                        </c:choose>
                                        <c:if test="${p.checkerComment != null}">
                                            <div class="extra-small mt-1 text-truncate ${p.status == 'APPROVED' ? 'text-muted' : (p.status == 'NEEDS_CORRECTION' ? 'text-warning' : 'text-danger')}" id="checker-comment-${p.id}" title="${p.checkerComment}">
                                                <c:if test="${p.status == 'APPROVED'}"><span class="text-success">&bull;</span></c:if>
                                                ${p.checkerComment}
                                            </div>
                                        </c:if>
                                    </td>
                                    <td class="col-th-creator"><span class="creator-tag" id="row-creator-${p.id}" title="${p.createdBy}">${p.createdBy != null ? p.createdBy : '-'}</span></td>
                                    <td class="text-end pe-3 col-th-actions">
                                        <div class="d-inline-flex justify-content-end align-items-center gap-1" id="actions-wrap-${p.id}">
                                            <!-- Preview Project Button -->
                                            <a href="${pageContext.request.contextPath}/project/${p.id}"
                                               class="btn btn-outline-primary btn-sm"
                                               id="link-preview-project-${p.id}">
                                                Preview
                                            </a>

                                            <!-- Maker: Edit / Submit -->
                                            <sec:authorize access="hasAnyRole('ZO_MAKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                                                <c:if test="${p.status == 'DRAFT' || p.status == 'NEEDS_CORRECTION'}">
                                                    <a href="${pageContext.request.contextPath}/admin/projects/${p.id}/edit"
                                                       class="btn btn-outline-secondary btn-sm"
                                                       id="link-edit-project-${p.id}">
                                                        Edit
                                                    </a>
                                                    <form action="${pageContext.request.contextPath}/admin/projects/${p.id}/submit" method="post" class="d-inline m-0" id="form-submit-project-${p.id}" data-confirm="Submit project for Checker approval?">
                                                        <button type="submit" class="btn btn-success btn-sm" id="btn-submit-project-${p.id}">Submit</button>
                                                    </form>
                                                </c:if>
                                            </sec:authorize>

                                            <!-- Checker: Approve / Reject / Correction -->
                                            <sec:authorize access="hasAnyRole('ZO_CHECKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                                                <c:if test="${p.status == 'PENDING_APPROVAL'}">
                                                    <button type="button" class="btn btn-warning btn-sm btn-review-project"
                                                            id="btn-review-project-${p.id}"
                                                            data-project-id="${p.id}"
                                                            data-project-name="${p.projectName}"
                                                            data-context-path="${pageContext.request.contextPath}">
                                                        Review
                                                    </button>
                                                </c:if>
                                            </sec:authorize>

                                            <!-- HO Retail: Reduce Units -->
                                            <sec:authorize access="hasAnyRole('HO_RETAIL', 'SUPERADMIN_DIT')">
                                                <c:if test="${p.status == 'APPROVED'}">
                                                    <button type="button" class="btn btn-outline-secondary btn-sm btn-reduce-units"
                                                            id="btn-reduce-units-${p.id}"
                                                            data-project-id="${p.id}"
                                                            data-project-name="${p.projectName}"
                                                            data-current-units="${p.unsoldInventory != null ? p.unsoldInventory : 0}"
                                                            data-context-path="${pageContext.request.contextPath}">
                                                        &minus; Units
                                                    </button>
                                                </c:if>
                                            </sec:authorize>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>

        <!-- Projects Pagination Footer -->
        <c:if test="${projectPage.totalPages > 1}">
            <div class="card-footer bg-white border-top d-flex justify-content-between align-items-center py-3 flex-wrap gap-2" id="footer-projects-pagination">
                <small class="text-muted" id="text-projects-page-info">
                    Showing page <strong>${projectPage.number + 1}</strong> of <strong>${projectPage.totalPages}</strong> (${projectPage.totalElements} total projects)
                </small>
                <nav aria-label="Projects pagination" id="nav-projects-pagination">
                    <ul class="pagination pagination-sm pagination-custom mb-0" id="ul-projects-pagination">
                        <li class="page-item <c:if test='${not projectPage.hasPrevious()}'>disabled</c:if>" id="li-proj-prev">
                            <a class="page-link" href="?page=${projectPage.number - 1}&status=${currentStatus}&search=${currentSearch}&zone=${currentZone}" id="link-proj-prev">&laquo; Prev</a>
                        </li>
                        <c:forEach var="i" begin="0" end="${projectPage.totalPages - 1}">
                            <li class="page-item <c:if test='${projectPage.number == i}'>active</c:if>" id="li-proj-page-${i}">
                                <a class="page-link" href="?page=${i}&status=${currentStatus}&search=${currentSearch}&zone=${currentZone}" id="link-proj-page-${i}">${i + 1}</a>
                            </li>
                        </c:forEach>
                        <li class="page-item <c:if test='${not projectPage.hasNext()}'>disabled</c:if>" id="li-proj-next">
                            <a class="page-link" href="?page=${projectPage.number + 1}&status=${currentStatus}&search=${currentSearch}&zone=${currentZone}" id="link-proj-next">Next &raquo;</a>
                        </li>
                    </ul>
                </nav>
            </div>
        </c:if>
    </div>
</main>

<!-- Checker Decision Modal (Bootstrap 5) -->
<div class="modal fade" id="decisionModal" tabindex="-1" aria-labelledby="decisionModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-primary" id="decisionModalLabel">
                    Checker Review: <span id="dialogProjectName"></span>
                </h5>
                <button type="button" class="btn-close" id="btn-close-decision-modal" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <form id="decisionForm" action="" method="post">
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label for="decisionSelect" class="form-label fw-semibold small">Decision *</label>
                        <select id="decisionSelect" name="decision" class="form-select" required>
                            <option value="approve">Approve &amp; Publish Project</option>
                            <option value="correction">Request Correction from Maker</option>
                            <option value="reject">Reject Project</option>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="checkerComment" class="form-label fw-semibold small">
                            Remarks / Reason <span id="commentReqText" class="text-danger d-none">* (Required for Correction/Rejection)</span>
                        </label>
                        <textarea id="checkerComment" name="comment" class="form-control" rows="3" placeholder="Enter remarks..."></textarea>
                    </div>
                </div>

                <div class="modal-footer border-top bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" id="btn-decision-cancel" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary btn-sm" id="btn-decision-confirm">Confirm Decision</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Reduce Units Modal (Bootstrap 5) -->
<div class="modal fade" id="reduceModal" tabindex="-1" aria-labelledby="reduceModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-primary" id="reduceModalLabel">Reduce Available Units</h5>
                <button type="button" class="btn-close" id="btn-close-reduce-modal" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <form id="reduceForm" action="" method="post">
                <div class="modal-body p-4">
                    <p class="text-muted small mb-3"><span id="reduceProjectName" class="fw-bold text-dark"></span></p>

                    <div class="mb-3">
                        <label for="reduceUnits" class="form-label fw-semibold small">
                            Units to Reduce * (Currently available: <span id="currentUnits" class="fw-bold text-success"></span>)
                        </label>
                        <input type="number" id="reduceUnits" name="units" class="form-control" min="1" required>
                    </div>

                    <div class="mb-3">
                        <label for="reduceReason" class="form-label fw-semibold small">Reason for Reduction *</label>
                        <textarea id="reduceReason" name="reason" class="form-control" rows="3" placeholder="e.g. Sold out through external channel or updated builder inventory" required></textarea>
                    </div>
                </div>

                <div class="modal-footer border-top bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" id="btn-reduce-cancel" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-danger btn-sm" id="btn-reduce-confirm">Confirm Reduction</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp"/>
<script id="admin-projects-js" src="${pageContext.request.contextPath}/assets/js/admin-projects.js"></script>
</body>
</html>