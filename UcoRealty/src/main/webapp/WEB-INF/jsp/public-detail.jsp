<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

                <c:set var="pageTitle" value="${project.projectName} - UCO Bank Realty" scope="request" />
                <c:set var="activeTab" value="public" scope="request" />
                <jsp:include page="common/header.jsp" />

                <main class="container py-4" id="main-project-detail">
                    <div class="mb-3">
                        <a href="${pageContext.request.contextPath}/projects" class="btn-back" id="btn-back-projects">
                            &larr; Back to All Housing Projects
                        </a>
                    </div>

                    <%-- Project Header Card --%>
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
                                    <c:if test="${not empty project.reraNo}">
                                        <span class="text-secondary" id="project-rera-info"><strong>RERA ID:</strong>
                                            <c:out value="${project.reraNo}"/></span>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </div>

                    <%-- Photos Gallery: an auto-sliding carousel (Bootstrap data attributes only, no inline script) --%>
                    <c:if test="${not empty images}">
                        <div class="card-custom mb-4" id="card-project-gallery">
                            <h2 class="card-title-custom" id="gallery-heading">Project Gallery</h2>
                            <div id="project-photo-carousel" class="carousel slide photo-carousel" data-bs-ride="carousel" data-bs-interval="4000" data-bs-pause="hover" aria-label="Project photos">
                                <c:if test="${fn:length(images) > 1}">
                                    <div class="carousel-indicators" id="gallery-indicators">
                                        <c:forEach var="img" items="${images}" varStatus="loop">
                                            <button type="button" data-bs-target="#project-photo-carousel" data-bs-slide-to="${loop.index}" class="${loop.first ? 'active' : ''}" ${loop.first ? 'aria-current="true"' : ''} aria-label="Photo ${loop.count}"></button>
                                        </c:forEach>
                                    </div>
                                </c:if>
                                <div class="carousel-inner" id="project-gallery-slides">
                                    <c:forEach var="img" items="${images}" varStatus="loop">
                                        <div class="carousel-item ${loop.first ? 'active' : ''}">
                                            <img src="${pageContext.request.contextPath}/files/<c:out value="${img}"/>"
                                                alt="Project Photo ${loop.count}" class="photo-carousel-img d-block w-100" ${loop.first ? '' : 'loading="lazy"'}
                                                id="gallery-photo-${img}">
                                        </div>
                                    </c:forEach>
                                </div>
                                <c:if test="${fn:length(images) > 1}">
                                    <button class="carousel-control-prev" type="button" data-bs-target="#project-photo-carousel" data-bs-slide="prev" id="gallery-prev">
                                        <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                                        <span class="visually-hidden">Previous photo</span>
                                    </button>
                                    <button class="carousel-control-next" type="button" data-bs-target="#project-photo-carousel" data-bs-slide="next" id="gallery-next">
                                        <span class="carousel-control-next-icon" aria-hidden="true"></span>
                                        <span class="visually-hidden">Next photo</span>
                                    </button>
                                    <span class="photo-counter" id="gallery-counter" aria-hidden="true">1 / ${fn:length(images)}</span>
                                </c:if>
                            </div>
                        </div>
                    </c:if>

                    <%-- Main Content Grid --%>
                    <div class="row g-4" id="project-content-grid">
                        <%-- Left Column: Specs --%>
                        <div class="col-lg-8" id="project-specs-column">
                            <%-- Overview & Pricing --%>
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
                                        <div class="text-muted small" title="Subject to availability">Total Units*</div>
                                        <div id="spec-total-inventory">${project.totalInventory != null ?
                                            project.totalInventory : '-'}</div>
                                    </div>
                                    <div class="col-sm-6 col-md-3">
                                        <div class="text-muted small" title="Subject to availability">Available Units*</div>
                                        <strong class="text-success fs-6"
                                            id="spec-unsold-inventory">${project.unsoldInventory != null ?
                                            project.unsoldInventory : 'Enquire'}</strong>
                                    </div>
                                    <div class="col-12 text-muted small" id="units-availability-note">* Subject to availability</div>
                                </div>
                            </div>

                            <%-- Property Specs --%>
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
                                            <c:if test="${not empty amenityRows || not empty project.amenities}">
                                                <tr class="amenity-row">
                                                    <th>Amenities</th>
                                                    <td id="spec-amenities">
                                                        <c:if test="${not empty amenityRows}">
                                                            <div class="amenity-grid" id="amenity-grid">
                                                                <c:forEach var="a" items="${amenityRows}" varStatus="vs">
                                                                    <div class="amenity-tile ${a.value == 'Yes' ? 'amenity-tile-yes' : 'amenity-tile-no'}" id="amenity-tile-${vs.index}">
                                                                        <span class="amenity-icon" aria-hidden="true"><c:out value="${empty amenityIcons[a.key] ? amenityDefaultIcon : amenityIcons[a.key]}"/></span>
                                                                        <span class="amenity-name"><c:out value="${a.key}"/></span>
                                                                        <span class="amenity-status"><c:out value="${a.value == 'Yes' ? 'Available' : 'Not available'}"/></span>
                                                                    </div>
                                                                </c:forEach>
                                                            </div>
                                                        </c:if>
                                                        <c:if test="${not empty project.amenities}"><div class="mt-2 small text-muted" id="amenity-other"><span class="fw-semibold">Other:</span> <c:out value="${project.amenities}"/></div></c:if>
                                                    </td>
                                                </tr>
                                            </c:if>
                                            <c:if test="${not empty project.videoUrl && fn:startsWith(project.videoUrl, 'http')}">
                                                <tr>
                                                    <th>Project Video</th>
                                                    <td><a href="<c:out value='${project.videoUrl}'/>" target="_blank" rel="noopener noreferrer" id="link-project-video">Watch Project Video &rarr;</a></td>
                                                </tr>
                                            </c:if>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <%-- Project Documents: one download card per document --%>
                            <c:if test="${not empty attachments}">
                                <div class="card-custom mb-4" id="card-project-documents">
                                    <h2 class="card-title-custom" id="documents-heading">Project Documents</h2>
                                    <div class="doc-grid" id="doc-list">
                                        <c:forEach var="att" items="${attachments}">
                                            <a class="doc-card" href="${pageContext.request.contextPath}/attachments/<c:out value='${att.id}'/>" download rel="noopener" id="link-attachment-${att.id}">
                                                <span class="doc-badge" aria-hidden="true">PDF</span>
                                                <span class="doc-info">
                                                    <span class="doc-name"><c:out value="${att.displayName}"/></span>
                                                    <span class="doc-meta">PDF document</span>
                                                </span>
                                                <span class="doc-action" aria-hidden="true">&#11015; Download</span>
                                            </a>
                                        </c:forEach>
                                    </div>
                                </div>
                            </c:if>

                            <%-- Builder Profile --%>
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
                                                    <td id="builder-rating-cell"><c:out value="${project.builderRating}"/><c:if test="${not empty project.builderRatingOutOf}"> / <c:out value="${project.builderRatingOutOf}"/></c:if></td>
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

                        <%-- Right Column: Home Loan Lead & Branch Contacts --%>
                        <div class="col-lg-4" id="project-loan-column">
                            <%-- Apply for Home Loan Card --%>
                            <div class="loan-apply-card" id="card-apply-home-loan">
                                <h2 class="card-title-custom border-0 pb-0 mb-2" id="loan-card-heading">Apply for Home Loan</h2>
                                <p class="text-muted small mb-3" id="loan-card-subheading">
                                    Choose how you would like to proceed.
                                </p>
                                <%-- Button 1: the bank's Home Loan enquiry page; ?utm=REALTY tells the bank the customer came from UCO Realty (our leads). Button 2: the bank's online application (login), a plain link. --%>
                                <div class="loan-actions loan-actions-stacked">
                                    <a href="<c:out value='${homeLoanUrl}'/>" class="btn btn-primary w-100 py-2 fw-bold loan-btn"
                                        id="btn-apply-loan-sidebar" rel="noopener">
                                        <span class="loan-btn-title">Enquire for Home Loan &rarr;</span>
                                        <span class="loan-btn-sub">Share your details, our team will contact you</span>
                                    </a>
                                    <a href="<c:out value='${homeLoanOnlineUrl}'/>" class="btn loan-btn-secondary w-100 py-2 fw-bold loan-btn"
                                        id="btn-apply-online-sidebar" target="_blank" rel="noopener noreferrer">
                                        <span class="loan-btn-title">Apply Online &rarr;</span>
                                        <span class="loan-btn-sub">Log in to the UCO Home Loan portal</span>
                                    </a>
                                </div>
                            </div>

                            <%-- UCO Bank Branch Assistance --%>
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

                    <%-- Second Apply for Home Loan button, at the bottom of the page (same link as the sidebar card) --%>
                    <div class="my-4" id="bottom-apply-home-loan">
                        <div class="loan-actions loan-actions-row">
                            <a href="<c:out value='${homeLoanUrl}'/>" class="btn btn-primary btn-lg fw-bold loan-btn" id="btn-apply-loan-bottom" rel="noopener">
                                <span class="loan-btn-title">Enquire for Home Loan &rarr;</span>
                                <span class="loan-btn-sub">Share your details, our team will contact you</span>
                            </a>
                            <a href="<c:out value='${homeLoanOnlineUrl}'/>" class="btn loan-btn-secondary btn-lg fw-bold loan-btn" id="btn-apply-online-bottom" target="_blank" rel="noopener noreferrer">
                                <span class="loan-btn-title">Apply Online &rarr;</span>
                                <span class="loan-btn-sub">Log in to the UCO Home Loan portal</span>
                            </a>
                        </div>
                    </div>
                </main>

                <jsp:include page="common/footer.jsp" />