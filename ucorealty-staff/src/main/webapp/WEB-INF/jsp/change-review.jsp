<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<c:set var="pageTitle" value="${isDelete ? 'Review Deletion' : (isCreate ? 'Review New Project' : 'Review Change')} - UCO Realty" scope="request" />
<c:set var="activeTab" value="projects" scope="request" />
<jsp:include page="common/header.jsp" />

<main class="container-xxl py-4" id="main-change-review">
    <div class="mb-3" id="review-top-nav">
        <a href="${pageContext.request.contextPath}/admin/projects" class="btn-back" id="btn-back-from-review">&larr; Back to Project Management</a>
        <a href="${pageContext.request.contextPath}/admin/projects/staging/<c:out value="${staging.id}"/>/preview" class="btn btn-outline-secondary btn-sm ms-2" id="btn-review-preview">Preview as customers would see it</a>
    </div>

    <div class="card-custom" id="card-change-review">
        <h2 class="card-title-custom" id="review-heading">
            ${isDelete ? 'Deletion request' : (isCreate ? 'New project' : 'Change request')}: <c:out value="${project.projectName}"/>
        </h2>

        <div class="row g-3 small mb-3" id="review-facts">
            <div class="col-md-3" id="fact-project-id"><span class="text-muted">Project</span><br><span class="font-monospace"><c:out value="${staging.projectId}"/></span></div>
            <div class="col-md-3" id="fact-zone"><span class="text-muted">Zone</span><br><c:out value="${project.zoneName}"/></div>
            <div class="col-md-3" id="fact-maker"><span class="text-muted">Raised by (maker)</span><br><c:out value="${staging.createdBy}"/></div>
            <div class="col-md-3" id="fact-submitted"><span class="text-muted">Sent for approval</span><br>${staging.submittedAt != null ? staging.submittedAt.toString().replace('T', ' ').substring(0, 16) : '-'}</div>
        </div>

        <c:choose>
            <c:when test="${isCreate}">
                <div class="alert alert-info small" id="review-new-note">
                    This is a <strong>new project</strong>. It exists only in the maker's working copy and is <strong>not live</strong>.
                    It is created in the live table, and appears on the customer site, only when you approve.
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info small" id="review-live-note">
                    The approved project is still <strong>live exactly as approved</strong> (customers see the left-hand values below).
                    Nothing changes until you approve.
                </div>
            </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${isDelete}">
                <div class="alert alert-danger" id="review-delete-box">
                    <strong>The maker asks to delete this project.</strong>
                    <div class="mt-1" id="review-delete-reason">Reason: <c:out value="${staging.reason}"/></div>
                    <div class="mt-2 small">After approval the project is marked <em>Deleted</em>: it disappears from every dashboard and from the customer site, and the record is kept in the database for the logs.</div>
                </div>
            </c:when>
            <c:when test="${isCreate}">
                <h3 class="form-section-title" id="review-contents-title">What the maker entered</h3>
                <div class="table-responsive">
                    <table class="table table-sm align-middle" id="table-new-project-contents">
                        <thead><tr><th id="th-content-field">Field</th><th id="th-content-value">Value</th></tr></thead>
                        <tbody>
                            <c:forEach var="d" items="${contents}" varStatus="st">
                                <tr id="row-content-${st.index}">
                                    <td id="content-field-${st.index}" class="fw-semibold"><c:out value="${d.label}"/></td>
                                    <td id="content-value-${st.index}"><c:out value="${d.newValue}"/></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <c:if test="${not empty newImages}">
                    <h3 class="form-section-title" id="review-new-photos-title">Photos</h3>
                    <div class="d-flex flex-wrap gap-3 mb-3" id="review-new-photos">
                        <c:forEach var="img" items="${newImages}">
                            <div id="new-photo-${img.id}"><img src="${pageContext.request.contextPath}/files/<c:out value="${img.id}"/>" alt="Photo" class="slot-preview-img" id="new-photo-img-${img.id}"></div>
                        </c:forEach>
                    </div>
                </c:if>
            </c:when>
            <c:otherwise>
                <c:if test="${not empty staging.reason}">
                    <p class="small" id="review-reason"><strong>Maker's reason:</strong> <c:out value="${staging.reason}"/></p>
                </c:if>

                <h3 class="form-section-title" id="review-diff-title">What changes</h3>
                <c:choose>
                    <c:when test="${empty diff}">
                        <p class="text-muted small" id="review-no-field-changes">No field values change (photos only).</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-sm align-middle" id="table-change-diff">
                                <thead>
                                    <tr><th id="th-diff-field">Field</th><th id="th-diff-old">Approved (live now)</th><th id="th-diff-new">Proposed</th></tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="d" items="${diff}" varStatus="st">
                                        <tr id="row-diff-${st.index}">
                                            <td id="diff-field-${st.index}" class="fw-semibold"><c:out value="${d.label}"/></td>
                                            <td id="diff-old-${st.index}" class="text-muted"><c:out value="${d.oldValue}"/></td>
                                            <td id="diff-new-${st.index}" class="text-success fw-semibold"><c:out value="${d.newValue}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>

                <c:if test="${not empty removedImages}">
                    <h3 class="form-section-title" id="review-removed-photos-title">Photos to be removed</h3>
                    <div class="d-flex flex-wrap gap-3 mb-3" id="review-removed-photos">
                        <c:forEach var="img" items="${removedImages}">
                            <div id="removed-photo-${img.id}"><img src="${pageContext.request.contextPath}/files/<c:out value="${img.id}"/>" alt="Photo to remove" class="slot-preview-img" id="removed-photo-img-${img.id}"></div>
                        </c:forEach>
                    </div>
                </c:if>
                <c:if test="${not empty newImages}">
                    <h3 class="form-section-title" id="review-new-photos-title">New photos</h3>
                    <div class="d-flex flex-wrap gap-3 mb-3" id="review-new-photos">
                        <c:forEach var="img" items="${newImages}">
                            <div id="new-photo-${img.id}"><img src="${pageContext.request.contextPath}/files/<c:out value="${img.id}"/>" alt="New photo" class="slot-preview-img" id="new-photo-img-${img.id}"></div>
                        </c:forEach>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${canDecide}">
                <form action="${pageContext.request.contextPath}/admin/projects/staging/<c:out value="${staging.id}"/>/decide" method="post" id="changeDecisionForm" class="border-top pt-3 mt-3">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="row g-3">
                        <div class="col-md-4">
                            <label for="changeDecisionSelect" class="form-label fw-semibold small">Decision *</label>
                            <select id="changeDecisionSelect" name="decision" class="form-select" required>
                                <option value="approve">${isDelete ? 'Approve deletion' : (isCreate ? 'Approve and publish project' : 'Approve and apply change')}</option>
                                <c:if test="${!isDelete}"><option value="needs-correction">Return to maker for correction</option></c:if>
                                <option value="reject">${isCreate ? 'Reject project' : 'Reject (project stays as approved)'}</option>
                            </select>
                        </div>
                        <div class="col-md-8">
                            <label for="changeDecisionComment" class="form-label fw-semibold small">Remarks <span class="text-muted">(required to return or reject)</span></label>
                            <textarea id="changeDecisionComment" name="comment" class="form-control" rows="2" placeholder="Enter remarks..."></textarea>
                        </div>
                    </div>
                    <div class="d-flex gap-2 mt-3">
                        <button type="submit" class="btn btn-primary" id="btn-change-decision-confirm" data-confirm="Record this decision?">Confirm Decision</button>
                        <a href="${pageContext.request.contextPath}/admin/projects" class="btn btn-outline-secondary" id="btn-change-decision-cancel">Cancel</a>
                    </div>
                </form>
            </c:when>
            <c:otherwise>
                <div class="border-top pt-3 mt-3" id="review-read-only">
                    <p class="small text-muted mb-2" id="review-read-only-note">
                        <c:choose>
                            <c:when test="${isOwner}">You raised this request; a checker has to decide it. You can withdraw it while it is waiting.</c:when>
                            <c:otherwise>You can view this request but not decide it.</c:otherwise>
                        </c:choose>
                    </p>
                    <c:if test="${isOwner}">
                        <form action="${pageContext.request.contextPath}/admin/projects/staging/<c:out value="${staging.id}"/>/withdraw" method="post" id="form-review-withdraw" data-confirm="Withdraw this request?">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button type="submit" class="btn btn-outline-secondary btn-sm" id="btn-review-withdraw">Withdraw request</button>
                        </form>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<jsp:include page="common/footer.jsp"/>
<script id="change-review-js" src="${pageContext.request.contextPath}/assets/js/change-review.js?v=${assetVersion}"></script>
