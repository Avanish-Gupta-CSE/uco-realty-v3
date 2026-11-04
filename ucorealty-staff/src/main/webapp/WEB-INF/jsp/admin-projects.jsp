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
        <div class="alert alert-success py-2 small mb-3" id="admin-alert-message"><c:out value="${message}"/></div>
    </c:if>
    <c:if test="${error != null}">
        <div class="alert alert-danger py-2 small mb-3" id="admin-alert-error"><c:out value="${error}"/></div>
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
                    <option value="PENDING_APPROVAL" ${currentStatus == 'PENDING_APPROVAL' ? 'selected' : ''}>Waiting for a Checker (all kinds)</option>
                    <option value="APPROVED" ${currentStatus == 'APPROVED' ? 'selected' : ''}>Approved</option>
                    <option value="NEEDS_CORRECTION" ${currentStatus == 'NEEDS_CORRECTION' ? 'selected' : ''}>Correction Required</option>
                    <option value="REJECTED" ${currentStatus == 'REJECTED' ? 'selected' : ''}>Rejected</option>
                    <option value="CHANGE_PENDING" ${currentStatus == 'CHANGE_PENDING' ? 'selected' : ''}>Change / Deletion Pending</option>
                </select>
            </div>

            <sec:authorize access="hasAnyRole('SUPERADMIN_DIT', 'HO_MARKETING', 'HO_RETAIL')">
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
            <a href="?status=CHANGE_PENDING&search=${currentSearch}&zone=${currentZone}" class="btn btn-sm ${currentStatus == 'CHANGE_PENDING' ? 'btn-primary' : 'btn-outline-secondary'}" id="filter-status-change-pending">Changes Pending</a>
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
                                <c:set var="mine" value="${p.stagingOwner == username}"/>
                                <c:set var="workable" value="${p.stagingStatus == 'DRAFT' || p.stagingStatus == 'NEEDS_CORRECTION'}"/>
                                <tr id="row-project-${p.id}">
                                    <td class="ps-3 col-th-project">
                                        <div class="project-title mb-1" id="row-name-${p.id}"><c:out value="${p.projectName}"/></div>
                                        <span class="project-badge-id font-monospace" id="row-id-${p.id}"><c:out value="${p.id}"/></span>
                                    </td>
                                    <td class="col-th-location">
                                        <div><c:choose><c:when test="${empty p.zoneName}"><span class="text-muted">Zone not set</span></c:when><c:otherwise><strong><c:out value="${p.zoneName}"/></strong> Zone</c:otherwise></c:choose></div>
                                        <small class="text-muted"><c:out value="${p.cityName}"/>, <c:out value="${p.stateName}"/></small>
                                    </td>
                                    <td class="col-th-builder"><c:out value="${p.builderName != null ? p.builderName : '-'}"/></td>
                                    <td class="col-th-inventory">
                                        <div><strong>${p.unsoldInventory != null ? p.unsoldInventory : 0}</strong> avail.</div>
                                        <small class="text-muted">${p.totalInventory != null ? p.totalInventory : 0} total units</small>
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
                                        <c:if test="${p.pendingKind == 'CHANGE_PENDING'}">
                                            <div class="mt-1"><span class="badge badge-pending" id="badge-change-${p.id}">Change pending approval</span></div>
                                        </c:if>
                                        <c:if test="${p.pendingKind == 'DELETE_PENDING'}">
                                            <div class="mt-1"><span class="badge badge-rejected" id="badge-delete-${p.id}">Deletion pending approval</span></div>
                                        </c:if>
                                        <c:if test="${p.master && mine && p.requestType == 'UPDATE' && p.stagingStatus == 'DRAFT'}">
                                            <div class="mt-1"><span class="badge badge-draft" id="badge-change-draft-${p.id}">Your change: draft</span></div>
                                        </c:if>
                                        <c:if test="${p.master && mine && p.requestType == 'UPDATE' && p.stagingStatus == 'NEEDS_CORRECTION'}">
                                            <div class="mt-1"><span class="badge badge-correction" id="badge-change-returned-${p.id}">Your change: returned</span>
                                                <div class="extra-small text-warning text-truncate" id="change-comment-${p.id}"><c:out value="${p.stagingComment}"/></div></div>
                                        </c:if>
                                        <c:if test="${p.checkerComment != null && (p.master || p.status == 'NEEDS_CORRECTION' || p.status == 'REJECTED')}">
                                            <div class="extra-small mt-1 text-truncate ${p.status == 'APPROVED' ? 'text-muted' : (p.status == 'NEEDS_CORRECTION' ? 'text-warning' : 'text-danger')}" id="checker-comment-${p.id}" title="<c:out value="${p.checkerComment}"/>">
                                                <c:if test="${p.status == 'APPROVED'}"><span class="text-success">&bull;</span></c:if>
                                                <c:out value="${p.checkerComment}"/>
                                            </div>
                                        </c:if>
                                    </td>
                                    <td class="col-th-creator"><span class="creator-tag" id="row-creator-${p.id}" title="<c:out value="${p.createdBy}"/>"><c:out value="${p.createdBy != null ? p.createdBy : '-'}"/></span></td>
                                    <td class="text-end pe-3 col-th-actions">
                                        <div class="d-inline-flex justify-content-end align-items-center gap-1" id="actions-wrap-${p.id}">
                                            <!-- Preview: a live project as customers see it; an unapproved one as it would look -->
                                            <a href="${pageContext.request.contextPath}<c:out value="${p.master ? '/project/'.concat(p.id) : '/admin/projects/staging/'.concat(p.stagingId).concat('/preview')}"/>"
                                               class="btn btn-outline-primary btn-sm" id="link-preview-project-${p.id}">Preview</a>

                                            <!-- Maker: his own NEW project (draft / returned / waiting) -->
                                            <sec:authorize access="hasAnyRole('ZO_MAKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                                                <c:if test="${!p.master && mine}">
                                                    <c:if test="${workable}">
                                                        <a href="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/edit" class="btn btn-outline-secondary btn-sm" id="link-edit-project-${p.id}">Edit</a>
                                                        <form action="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/submit" method="post" class="d-inline m-0" id="form-submit-project-${p.id}" data-confirm="Submit this project for Checker approval?">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                            <button type="submit" class="btn btn-success btn-sm" id="btn-submit-project-${p.id}">Submit</button>
                                                        </form>
                                                    </c:if>
                                                    <c:if test="${workable || p.stagingStatus == 'PENDING_APPROVAL'}">
                                                        <form action="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/withdraw" method="post" class="d-inline m-0" id="form-withdraw-new-${p.id}" data-confirm="${workable ? 'Discard this project draft?' : 'Withdraw this project from approval?'}">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                            <button type="submit" class="btn btn-outline-secondary btn-sm" id="btn-withdraw-new-${p.id}">${workable ? 'Discard' : 'Withdraw'}</button>
                                                        </form>
                                                    </c:if>
                                                </c:if>

                                                <!-- Maker: change a LIVE project (every change needs a checker) -->
                                                <c:if test="${p.master}">
                                                    <c:choose>
                                                        <c:when test="${p.stagingId == null}">
                                                            <a href="${pageContext.request.contextPath}/admin/projects/<c:out value="${p.id}"/>/change" class="btn btn-outline-secondary btn-sm" id="link-change-project-${p.id}">Edit</a>
                                                            <button type="button" class="btn btn-outline-secondary btn-sm btn-reduce-units"
                                                                    id="btn-reduce-units-${p.id}"
                                                                    data-project-id="${p.id}"
                                                                    data-project-name="<c:out value="${p.projectName}"/>"
                                                                    data-current-units="${p.unsoldInventory != null ? p.unsoldInventory : 0}"
                                                                    data-context-path="${pageContext.request.contextPath}">Reduce Units</button>
                                                            <button type="button" class="btn btn-outline-danger btn-sm btn-delete-project"
                                                                    id="btn-delete-project-${p.id}"
                                                                    data-project-id="${p.id}"
                                                                    data-project-name="<c:out value="${p.projectName}"/>"
                                                                    data-context-path="${pageContext.request.contextPath}">Delete</button>
                                                        </c:when>
                                                        <c:when test="${mine && p.requestType == 'UPDATE' && workable}">
                                                            <a href="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/edit" class="btn btn-warning btn-sm" id="link-continue-change-${p.id}">Continue change</a>
                                                            <form action="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/withdraw" method="post" class="d-inline m-0" id="form-withdraw-change-${p.id}" data-confirm="Withdraw this change? The approved project stays as it is.">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                                <button type="submit" class="btn btn-outline-secondary btn-sm" id="btn-withdraw-change-${p.id}">Withdraw</button>
                                                            </form>
                                                        </c:when>
                                                        <c:when test="${mine && p.stagingStatus == 'PENDING_APPROVAL'}">
                                                            <form action="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/withdraw" method="post" class="d-inline m-0" id="form-withdraw-pending-${p.id}" data-confirm="Withdraw this request? The approved project stays as it is.">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                                                <button type="submit" class="btn btn-outline-secondary btn-sm" id="btn-withdraw-pending-${p.id}">Withdraw request</button>
                                                            </form>
                                                        </c:when>
                                                    </c:choose>
                                                </c:if>
                                            </sec:authorize>

                                            <!-- Checker: review whatever is waiting (new project, change or deletion), not his own -->
                                            <sec:authorize access="hasAnyRole('ZO_CHECKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                                                <c:if test="${p.waitingForChecker && !mine}">
                                                    <a href="${pageContext.request.contextPath}/admin/projects/staging/${p.stagingId}/review" class="btn btn-warning btn-sm" id="link-review-${p.id}">
                                                        ${p.requestType == 'DELETE' ? 'Review deletion' : (p.requestType == 'UPDATE' ? 'Review change' : 'Review')}
                                                    </a>
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

<!-- Reduce Units Modal (Bootstrap 5) -->
<div class="modal fade" id="reduceModal" tabindex="-1" aria-labelledby="reduceModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-primary" id="reduceModalLabel">Request Unit Reduction</h5>
                <button type="button" class="btn-close" id="btn-close-reduce-modal" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <form id="reduceForm" action="" method="post">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="modal-body p-4">
                    <p class="text-muted small mb-3"><span id="reduceProjectName" class="fw-bold text-dark"></span></p>
                    <p class="small text-muted mb-3" id="reduce-approval-note">This is an edit of an approved project, so it goes to a checker. The live unit count changes only after approval.</p>

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
                    <button type="submit" class="btn btn-danger btn-sm" id="btn-reduce-confirm">Send for Approval</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Delete Project Modal (needs checker approval) -->
<div class="modal fade" id="deleteModal" tabindex="-1" aria-labelledby="deleteModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-danger" id="deleteModalLabel">Request Deletion</h5>
                <button type="button" class="btn-close" id="btn-close-delete-modal" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form id="deleteForm" action="" method="post">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="modal-body p-4">
                    <p class="text-muted small mb-3"><span id="deleteProjectName" class="fw-bold text-dark"></span></p>
                    <p class="small text-muted mb-3" id="delete-approval-note">The project stays live until a checker approves the deletion. After approval it disappears from the dashboard and the customer site; the record is kept for the logs.</p>
                    <div class="mb-3">
                        <label for="deleteReason" class="form-label fw-semibold small">Reason for deletion *</label>
                        <textarea id="deleteReason" name="reason" class="form-control" rows="3" placeholder="Why should this project be removed?" required></textarea>
                    </div>
                </div>
                <div class="modal-footer border-top bg-light">
                    <button type="button" class="btn btn-secondary btn-sm" id="btn-delete-cancel" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-danger btn-sm" id="btn-delete-confirm">Send for Approval</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp"/>
<script id="admin-projects-js" src="${pageContext.request.contextPath}/assets/js/admin-projects.js?v=${assetVersion}"></script>
</body>
</html>