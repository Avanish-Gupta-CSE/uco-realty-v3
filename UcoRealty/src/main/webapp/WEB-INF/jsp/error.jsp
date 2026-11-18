<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%-- Shown by Spring Boot for errors that happen before any page code runs (unknown address, blocked request, system error).
     Without this file the browser would show Spring's plain "Whitelabel Error Page". --%>
<c:set var="pageTitle" value="Page not available - UCO Bank Realty" scope="request"/>
<jsp:include page="common/header.jsp"/>
<c:set var="sc" value="${empty status ? requestScope['jakarta.servlet.error.status_code'] : status}"/>

<main class="container py-5 text-center" id="main-error">
    <h2 class="h4 text-primary fw-bold mb-3" id="error-title">We could not open this page</h2>
    <p class="text-muted mb-4" id="error-message">
        <c:choose>
            <c:when test="${sc == 404 || sc == 403}">The page you asked for is not available.</c:when>
            <c:when test="${sc == 400 || sc == 405}">The request was not valid. Please go back and try again.</c:when>
            <c:otherwise>Something went wrong on our side. Please try again in a little while.</c:otherwise>
        </c:choose>
    </p>
    <a href="${pageContext.request.contextPath}/projects" class="btn btn-primary" id="btn-error-home">Browse Approved Projects</a>
</main>

<jsp:include page="common/footer.jsp"/>
