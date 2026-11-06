<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
            <%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

                <c:set var="pageTitle" value="${project.projectName} - UCO Bank Realty" scope="request" />
                <c:set var="activeTab" value="public" scope="request" />
                <jsp:include page="common/header.jsp" />

                <main class="container-xxl py-4" id="main-project-detail">
                    <div class="mb-3">
                        <a href="${pageContext.request.contextPath}/projects" class="btn-back" id="btn-back-projects">
                            &larr; Back to All Housing Projects
                        </a>
                    </div>

                    <!-- Staff Preview Warning Banner -->
                    <c:if test="${isStaffPreview}">
                        <div class="staff-preview-banner" id="staff-preview-banner">
                            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                                <div>
                                    <strong>⚠️ Staff Internal Preview Mode</strong>
                                    <div class="small mt-1 text-dark">
                                        This project is currently in <strong>${project.status}</strong> status. It is
                                        <strong>NOT</strong> visible to public customers until approved by a Checker.
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <a href="${pageContext.request.contextPath}/admin/projects"
                                        class="btn btn-secondary btn-sm" id="btn-preview-back-admin">&larr; Back to
                                        Admin Portal</a>
                                    <sec:authorize access="hasAnyRole('ZO_MAKER', 'HO_MARKETING', 'SUPERADMIN_DIT')">
                                        <c:if
                                            test="${project.status == 'DRAFT' || project.status == 'NEEDS_CORRECTION'}">
                                            <a href="${pageContext.request.contextPath}/admin/projects/staging/${stagingId}/edit"
                                                class="btn btn-primary btn-sm" id="btn-preview-edit-project">Edit
                                                Project</a>
                                        </c:if>
                                    </sec:authorize>
                                </div>
                            </div>
                        </div>
                    </c:if>

                    <!-- Project Header Card -->
                    <div class="project-header-card" id="card-project-header">
                        <div class="d-flex justify-content-between align-items-start flex-wrap gap-3">
                            <div>
                                <h1 class="h3 fw-bold text-primary mb-1" id="project-detail-title">
                                    <c:out value="${project.projectName}"/></h1>
                                <div class="text-muted mb-2" id="project-detail-builder">
                                    by <strong><c:out value="${project.builderName != null ? project.builderName : 'Not
                                        specified'}"/></strong>
                                </div>
                                <div class="d-flex gap-2 flex-wrap align-items-center small">
                                    <c:choose>
                                        <c:when test="${project.status == 'APPROVED'}">
                                            <span class="badge badge-approved" id="project-detail-status-badge">UCO
                                                Approved</span>
                                        </c:when>
                                        <c:when test="${project.status == 'PENDING_APPROVAL'}">
                                            <span class="badge badge-pending" id="project-detail-status-badge">Pending
                                                Approval</span>
                                        </c:when>
                                        <c:when test="${project.status == 'DRAFT'}">
                                            <span class="badge badge-draft"
                                                id="project-detail-status-badge">Draft</span>
                                        </c:when>
                                        <c:when test="${project.status == 'NEEDS_CORRECTION'}">
                                            <span class="badge badge-correction" id="project-detail-status-badge">Needs
                                                Correction</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-draft"
                                                id="project-detail-status-badge">${project.status}</span>
                                        </c:otherwise>
                                    </c:choose>
                                    <span class="text-secondary" id="project-location-info"><strong>Location:</strong>
                                        <c:out value="${project.cityName}"/>, <c:out value="${project.stateName}"/> <c:out value="${project.pinCode != null ? ('- ' +=
                                        project.pinCode) : ''}"/></span>
                                    <c:if test="${project.reraNo != null}">
                                        <span class="text-secondary" id="project-rera-info"><strong>RERA ID:</strong>
                                            <c:out value="${project.reraNo}"/></span>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Photos Gallery -->
                    <c:if test="${not empty images}">
                        <div class="card-custom mb-4" id="card-project-gallery">
                            <h2 class="card-title-custom" id="gallery-heading">Project Gallery</h2>
                            <div class="project-gallery-scroller" id="project-gallery-scroller">
                                <c:forEach var="img" items="${images}" varStatus="loop">
                                    <img src="${pageContext.request.contextPath}/files/<c:out value="${img.id}"/>"
                                        alt="Project Photo ${loop.count}" class="gallery-img"
                                        id="gallery-photo-${img.id}">
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>

                    <!-- Main Content Grid -->
                    <div class="row g-4" id="project-content-grid">
                        <!-- Left Column: Specs -->
                        <div class="col-lg-8" id="project-specs-column">
                            <!-- Overview & Pricing -->
                            <div class="card-custom mb-4" id="card-pricing-overview">
                                <h2 class="card-title-custom" id="pricing-heading">Pricing &amp; Inventory</h2>
                                <div class="row g-3">
                                    <div class="col-sm-6 col-md-3">
                                        <div class="text-muted small">Price Range</div>
                                        <strong class="text-primary fs-6" id="spec-price-range">
                                            <c:out value="${project.budgetMin != null ? project.budgetMin : 'Contact'}"/>
                                            <c:out value="${project.budgetMin != null && project.budgetMax != null ? '–' : ''}"/>
                                            <c:out value="${project.budgetMax != null ? project.budgetMax : ''}"/>
                                        </strong>
                                    </div>
                                    <div class="col-sm-6 col-md-3">
                                        <div class="text-muted small">Average Rate</div>
                                        <div id="spec-avg-rate"><c:out value="${project.avgRate != null ? project.avgRate : 'On request'}"/></div>
                                    </div>
                                    <div class="col-sm-6 col-md-3">
                                        <div class="text-muted small">Total Units</div>
                                        <div id="spec-total-inventory">${project.totalInventory != null ?
                                            project.totalInventory : '-'}</div>
                                    </div>
                                    <div class="col-sm-6 col-md-3">
                                        <div class="text-muted small">Available Units</div>
                                        <strong class="text-success fs-6"
                                            id="spec-unsold-inventory">${project.unsoldInventory != null ?
                                            project.unsoldInventory : 'Enquire'}</strong>
                                    </div>
                                </div>
                            </div>

                            <!-- Property Specs -->
                            <div class="card-custom mb-4" id="card-property-specs">
                                <h2 class="card-title-custom" id="specs-heading">Property Details &amp; Specifications
                                </h2>
                                <div class="table-responsive">
                                    <table class="table table-bordered table-spec mb-0" id="table-property-specs">
                                        <tbody>
                                            <c:if test="${not empty project.typeOfUnit}">
                                            <tr>
                                                <th>Type of Units</th>
                                                <td id="spec-unit-type"><c:out value="${project.typeOfUnit != null ?
                                                    project.typeOfUnit : 'Not provided'}"/></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.totalProjectArea}">
                                            <tr>
                                                <th>Total Project Area</th>
                                                <td id="spec-total-area"><c:out value="${project.totalProjectArea != null ?
                                                    project.totalProjectArea : 'Not provided'}"/></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.carpetMin || not empty project.carpetMax}">
                                            <tr>
                                                <th>Carpet Area</th>
                                                <td id="spec-carpet-area">
                                                    <c:out value="${project.carpetMin != null ? (project.carpetMin += ' sq.ft.') :
                                                    ''}"/>
                                                    <c:out value="${project.carpetMin != null && project.carpetMax != null ? '–' : ''}"/>
                                                    <c:out value="${project.carpetMax != null ? (project.carpetMax += ' sq.ft.') : ''}"/>
                                                </td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.buildupMin || not empty project.buildupMax}">
                                            <tr>
                                                <th>Super Built-up Area</th>
                                                <td id="spec-buildup-area">
                                                    <c:out value="${project.buildupMin != null ? (project.buildupMin += ' sq.ft.') :
                                                    ''}"/>
                                                    <c:out value="${project.buildupMin != null && project.buildupMax != null ? '–' :
                                                    ''}"/>
                                                    <c:out value="${project.buildupMax != null ? (project.buildupMax += ' sq.ft.') :
                                                    ''}"/>
                                                </td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${project.noOfTowers != null || project.aptPerFloor != null}">
                                            <tr>
                                                <th>Towers &amp; Floors</th>
                                                <td id="spec-towers-floors"><c:if test="${project.noOfTowers != null}"><c:out value="${project.noOfTowers}"/> towers</c:if><c:if test="${project.noOfTowers != null && project.aptPerFloor != null}">, </c:if><c:if test="${project.aptPerFloor != null}"><c:out value="${project.aptPerFloor}"/> units per floor</c:if></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.distRailway}">
                                            <tr>
                                                <th>Distance to Railway</th>
                                                <td id="spec-dist-railway"><c:out value="${project.distRailway != null ?
                                                    project.distRailway : 'Not provided'}"/></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.distAirport}">
                                            <tr>
                                                <th>Distance to Airport</th>
                                                <td id="spec-dist-airport"><c:out value="${project.distAirport != null ?
                                                    project.distAirport : 'Not provided'}"/></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${not empty project.projectAddress}">
                                            <tr>
                                                <th>Project Address</th>
                                                <td id="spec-project-address"><c:out value="${project.projectAddress != null ?
                                                    project.projectAddress : 'Not provided'}"/></td>
                                            </tr>
                                            </c:if>
                                            <c:if test="${project.amenities != null}">
                                                <tr>
                                                    <th>Amenities</th>
                                                    <td id="spec-amenities"><c:out value="${project.amenities}"/></td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <!-- Builder Profile -->
                            <div class="card-custom mb-4" id="card-builder-profile">
                                <h2 class="card-title-custom" id="builder-heading">Builder Details</h2>
                                <div class="table-responsive">
                                    <table class="table table-bordered table-spec mb-0" id="table-builder-details">
                                        <tbody>
                                            <tr>
                                                <th>Builder / Developer</th>
                                                <td id="builder-name-cell"><strong><c:out value="${project.builderName != null ?
                                                        project.builderName : 'Not provided'}"/></strong></td>
                                            </tr>
                                            <c:if test="${project.website != null}">
                                                <tr>
                                                    <th>Website</th>
                                                    <td><a href="<c:out value="${project.website}"/>"
                                                            id="builder-website-link"><c:out value="${project.website}"/></a></td>
                                                </tr>
                                            </c:if>
                                            <c:if test="${project.builderRating != null}">
                                                <tr>
                                                    <th>Rating</th>
                                                    <td id="builder-rating-cell"><c:out value="${project.builderRating}"/></td>
                                                </tr>
                                            </c:if>
                                            <c:if
                                                test="${project.salesExecName != null || project.salesMobile != null}">
                                                <tr>
                                                    <th>Sales Executive</th>
                                                    <td id="builder-sales-cell"><c:out value="${project.salesExecName}"/>
                                                        <c:out value="${project.salesMobile != null ? ('(' += project.salesMobile +=
                                                        ')') : ''}"/></td>
                                                </tr>
                                            </c:if>
                                            <c:if test="${project.builderBg != null}">
                                                <tr>
                                                    <th>Background</th>
                                                    <td id="builder-bg-cell"><c:out value="${project.builderBg}"/></td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        <!-- Right Column: Home Loan Lead & Branch Contacts -->
                        <div class="col-lg-4" id="project-loan-column">
                            <!-- Apply for Home Loan Card -->
                            <div class="loan-apply-card" id="card-apply-home-loan">
                                <h2 class="card-title-custom border-0 pb-0 mb-2" id="loan-card-heading">Apply for Home Loan</h2>
                                <p class="text-muted small mb-3" id="loan-card-subheading">
                                    Fast-track loan processing with preferential interest rates for this approved
                                    project.
                                </p>
                                <button type="button" class="btn btn-primary w-100 py-2 fw-bold"
                                    id="btn-apply-loan-sidebar" data-action="apply-loan"
                                    data-project-id="${project.id}">
                                    Apply for Home Loan &rarr;
                                </button>
                            </div>

                            <!-- UCO Bank Branch Assistance -->
                            <div class="card-custom" id="card-branch-assistance">
                                <h2 class="card-title-custom" id="branch-assistance-heading">Bank Branch Assistance</h2>
                                <div class="text-muted small" id="branch-details-wrap">
                                    <p class="mb-2" id="branch-name-info">
                                        <strong>Attached Branch:</strong><br>
                                        <c:out value="${project.branchName != null ? project.branchName : 'Local UCO Branch'}"/>
                                        (<c:out value="${project.zoneName}"/> Zone)
                                    </p>
                                    <c:if test="${project.branchHeadName != null}">
                                        <p class="mb-2" id="branch-head-info">
                                            <strong>Branch Head:</strong><br>
                                            <c:out value="${project.branchHeadName}"/> <c:out value="${project.branchHeadNumber != null ? ('- ' +=
                                            project.branchHeadNumber) : ''}"/>
                                        </p>
                                    </c:if>
                                    <c:if test="${project.relExecName != null}">
                                        <p class="mb-0" id="branch-relexec-info">
                                            <strong>Relationship Executive:</strong><br>
                                            <c:out value="${project.relExecName}"/> <c:out value="${project.relExecNumber != null ? ('- ' +=
                                            project.relExecNumber) : ''}"/>
                                        </p>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>

                <jsp:include page="common/footer.jsp" />