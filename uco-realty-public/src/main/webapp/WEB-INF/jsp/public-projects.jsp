<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

<c:set var="pageTitle" value="Approved Housing Projects - UCO Bank" scope="request"/>
<c:set var="activeTab" value="public" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container py-4" id="main-public-catalog">
    <div class="hero-banner" id="public-hero-banner">
        <h2 id="hero-title">Explore UCO Bank Approved Housing Projects</h2>
        <p id="hero-subtitle">
            Choose from premium residential properties approved for swift UCO Home Loan processing with attractive interest rates and minimal paperwork.
        </p>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar mb-4" id="public-filter-container">
        <form action="${pageContext.request.contextPath}/projects" method="get" class="row g-3 align-items-end" id="form-public-filter">
            <div class="col-md-3">
                <label for="filter-state" class="form-label fw-semibold small">State</label>
                <select id="filter-state" name="state" class="form-select form-select-sm">
                    <option value="">-- All States --</option>
                    <c:forEach var="st" items="${states}">
                        <option value="${st}" ${param.state == st ? 'selected' : ''}>${st}</option>
                    </c:forEach>
                </select>
            </div>

            <div class="col-md-3">
                <label for="filter-city" class="form-label fw-semibold small">City / District</label>
                <select id="filter-city" name="city" class="form-select form-select-sm">
                    <option value="">-- All Cities --</option>
                    <c:forEach var="ct" items="${cities}">
                        <option value="${ct}" ${param.city == ct ? 'selected' : ''}>${ct}</option>
                    </c:forEach>
                </select>
            </div>

            <div class="col-md-4">
                <label for="filter-search" class="form-label fw-semibold small">Search (Project, Builder, RERA, Pincode)</label>
                <input type="text" id="filter-search" name="search" class="form-control form-control-sm" value="${param.search}" placeholder="e.g. Green Valley or 700001">
            </div>

            <div class="col-md-2 d-flex gap-2">
                <button type="submit" class="btn btn-primary btn-sm flex-fill" id="btn-filter-apply">Apply</button>
                <a href="${pageContext.request.contextPath}/projects" class="btn btn-secondary btn-sm flex-fill" id="btn-filter-reset">Reset</a>
            </div>
        </form>
    </div>

    <!-- Projects Grid -->
    <c:choose>
        <c:when test="${empty projects}">
            <div class="card-custom text-center py-5" id="card-no-projects">
                <h3 class="text-secondary mb-2" id="no-projects-title">No Approved Projects Found</h3>
                <p class="text-muted small" id="no-projects-desc">Try clearing or adjusting your search filters to view available housing projects.</p>
                <a href="${pageContext.request.contextPath}/projects" class="btn btn-primary btn-sm mt-3" id="btn-view-all-projects">View All Projects</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="mb-3 small text-muted" id="projects-count-display">
                Showing <strong id="count-number">${fn:length(projects)}</strong> approved property projects
            </div>

            <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4" id="projects-grid-container">
                <c:forEach var="p" items="${projects}">
                    <div class="col">
                        <div class="card h-100 project-card shadow-sm" id="project-card-${p.id}">
                            <c:choose>
                                <c:when test="${projectThumbnails[p.id] != null}">
                                    <img src="${pageContext.request.contextPath}/files/${projectThumbnails[p.id]}" alt="${p.projectName}" class="project-card-img" id="project-thumb-${p.id}">
                                </c:when>
                                <c:otherwise>
                                    <div class="project-card-img-placeholder" id="project-thumb-placeholder-${p.id}">
                                        No Image Available
                                    </div>
                                </c:otherwise>
                            </c:choose>

                            <div class="card-body d-flex flex-column p-3">
                                <h3 class="project-title" id="project-title-${p.id}">${p.projectName}</h3>
                                <div class="project-builder" id="project-builder-${p.id}">by ${p.builderName != null ? p.builderName : 'Not specified'}</div>

                                <div class="project-meta-row">
                                    <span class="text-muted">Location:</span>
                                    <strong>${p.cityName}, ${p.stateName}</strong>
                                </div>

                                <c:if test="${p.pinCode != null}">
                                    <div class="project-meta-row">
                                        <span class="text-muted">Pincode:</span>
                                        <span>${p.pinCode}</span>
                                    </div>
                                </c:if>

                                <c:if test="${p.budgetMin != null || p.budgetMax != null}">
                                    <div class="project-meta-row">
                                        <span class="text-muted">Price Range:</span>
                                        <strong class="text-primary">
                                            ${p.budgetMin != null ? p.budgetMin : ''} ${p.budgetMin != null && p.budgetMax != null ? '–' : ''} ${p.budgetMax != null ? p.budgetMax : ''}
                                        </strong>
                                    </div>
                                </c:if>

                                <div class="project-meta-row">
                                    <span class="text-muted">Available Units:</span>
                                    <span class="text-success fw-bold">
                                        ${p.unsoldInventory != null ? p.unsoldInventory : 'Enquire'}
                                    </span>
                                </div>

                                <div class="mt-auto pt-3">
                                    <a href="${pageContext.request.contextPath}/project/${p.id}" class="btn btn-primary w-100" id="btn-view-project-${p.id}">
                                        View Project Details &rarr;
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<jsp:include page="common/footer.jsp"/>
