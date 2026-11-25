<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<c:set var="pageTitle" value="Home - UCO Bank Realty Portal" scope="request"/>
<c:set var="activeTab" value="home" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container py-4" id="main-home">
    <section class="home-hero" id="home-hero">
        <h2 id="home-hero-title">Find your home with UCO Bank</h2>
        <p id="home-hero-text">
            Browse housing projects that UCO Bank has approved, see their photos, details and documents, and reach the bank's Home Loan application from the same place.
        </p>
        <div class="home-hero-actions">
            <a href="${pageContext.request.contextPath}/projects" class="btn btn-light btn-lg fw-bold" id="btn-home-browse">Browse Approved Projects &rarr;</a>
            <a href="${pageContext.request.contextPath}/about" class="btn btn-outline-light btn-lg fw-bold" id="btn-home-about">About Us</a>
        </div>
    </section>

    <h3 class="home-section-title" id="home-steps-title">How it works</h3>
    <div class="row g-3 mb-4" id="home-steps">
        <div class="col-md-4">
            <div class="home-step" id="home-step-1">
                <span class="home-step-icon" aria-hidden="true">&#128269;</span>
                <h4>1. Browse projects</h4>
                <p>Open <strong>Projects</strong> and search by state, city, project or builder.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="home-step" id="home-step-2">
                <span class="home-step-icon" aria-hidden="true">&#127970;</span>
                <h4>2. See the details</h4>
                <p>Open a project to see its photos, specifications, amenities and documents such as the brochure and floor plan.</p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="home-step" id="home-step-3">
                <span class="home-step-icon" aria-hidden="true">&#127969;</span>
                <h4>3. Apply for a Home Loan</h4>
                <p>Start your Home Loan journey by sharing your details, or apply online on the bank's Home Loan portal.</p>
            </div>
        </div>
    </div>

    <div class="card-custom" id="home-approved-note">
        <h3 class="card-title-custom" id="home-approved-title">Projects approved by the bank</h3>
        <p class="mb-0 text-muted" id="home-approved-text">
            Every project listed here carries the <span class="badge badge-approved">UCO Approved</span> mark, so you can explore your options with confidence.
        </p>
    </div>
</main>

<jsp:include page="common/footer.jsp"/>
