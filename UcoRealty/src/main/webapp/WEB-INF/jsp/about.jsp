<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<c:set var="pageTitle" value="About Us - UCO Bank Realty Portal" scope="request"/>
<c:set var="activeTab" value="about" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container py-4" id="main-about">
    <section class="about-banner" id="about-banner">
        <p class="about-banner-kicker" id="about-banner-kicker">UCO Bank Realty Portal</p>
        <h2 id="about-banner-title">Homes approved by UCO Bank, in one place</h2>
    </section>

    <section class="about-who" id="about-who">
        <div class="about-text" id="about-text">
            <h3 id="about-who-title">Who are we?</h3>
            <p id="about-text-1">
                UCO Bank Realty Portal brings housing projects approved by UCO Bank into one place. Each project shows its photos, specifications,
                amenities and documents such as the brochure and floor plan, so a buyer can compare options before applying.
            </p>
            <p id="about-text-2">
                A project appears here only after it has been checked and approved by the bank. From any project page you can enquire about a
                UCO Home Loan or continue to the bank's online Home Loan application.
            </p>
        </div>

        <div class="about-points" id="about-points">
            <div class="about-point" id="about-point-1">
                <span class="about-point-icon" aria-hidden="true">&#9989;</span>
                <div>
                    <h4>Approved by the bank</h4>
                    <p>Every project carries the UCO Approved mark.</p>
                </div>
            </div>
            <div class="about-point" id="about-point-2">
                <span class="about-point-icon" aria-hidden="true">&#128247;</span>
                <div>
                    <h4>Photos, details and documents</h4>
                    <p>See the gallery, the specifications, the amenities and download the brochure.</p>
                </div>
            </div>
            <div class="about-point" id="about-point-3">
                <span class="about-point-icon" aria-hidden="true">&#127969;</span>
                <div>
                    <h4>Straight to the Home Loan</h4>
                    <p>Enquire for a Home Loan or apply online from the project page.</p>
                </div>
            </div>
        </div>
    </section>
</main>

<jsp:include page="common/footer.jsp"/>
