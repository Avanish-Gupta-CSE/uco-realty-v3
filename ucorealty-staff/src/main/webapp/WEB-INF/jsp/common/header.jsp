<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${pageTitle != null ? pageTitle : 'UCO Bank - Approved Housing Projects'}"/></title>
    <link rel="icon" type="image/png" id="favicon-link" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <!-- Bootstrap 5.3.2 CSS (Bank Standard) -->
    <link rel="stylesheet" id="bootstrap-css" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
    <!-- UCO Bank Theme Stylesheet -->
    <%-- bump assetVersion whenever CSS/JS change, so browsers do not keep showing the old design --%>
    <c:set var="assetVersion" value="20261104b" scope="request" />
    <link rel="stylesheet" id="uco-theme-css" href="${pageContext.request.contextPath}/assets/css/style.css?v=${assetVersion}">
</head>
<body>

<header class="header" id="app-header">
    <div class="container">
        <div class="header-top">
            <div class="logo-container" id="logo-container">
                <a href="${pageContext.request.contextPath}/" id="link-home-logo">
                    <img src="${pageContext.request.contextPath}/assets/images/uco_bank_logo.png" alt="UCO Bank Logo" class="logo-img" id="img-bank-logo">
                </a>
                <div class="logo-text" id="logo-text">
                    <h1 id="app-title-heading">UCO Bank Realty Portal</h1>
                    <p id="app-subtitle">Approved Housing Projects for Home Loans</p>
                </div>
            </div>

            <div class="user-menu" id="user-menu-section">
                <sec:authorize access="!isAuthenticated()">
                    <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-light btn-sm" id="btn-staff-login">
                        Staff Login
                    </a>
                </sec:authorize>
                <sec:authorize access="isAuthenticated()">
                    <span id="user-info-text">Signed in as: <strong id="user-username"><sec:authentication property="name"/></strong></span>
                    <span class="user-badge" id="badge-user-role"><sec:authentication property="principal.authorities"/></span>
                    <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline m-0" id="form-nav-logout">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button type="submit" class="btn btn-danger btn-sm" id="btn-nav-logout">Logout</button>
                    </form>
                </sec:authorize>
            </div>
        </div>
    </div>

    <nav class="navbar navbar-expand navbar-custom" id="main-navigation">
        <div class="container">
            <div class="navbar-nav" id="main-navbar-nav">
                <a href="${pageContext.request.contextPath}/projects" class="nav-link ${activeTab == 'public' ? 'active' : ''}" id="nav-link-customer-portal">Customer Portal</a>
                <sec:authorize access="isAuthenticated()">
                    <a href="${pageContext.request.contextPath}/admin/projects" class="nav-link ${activeTab == 'projects' ? 'active' : ''}" id="nav-link-project-mgmt">Project Management</a>
                    <sec:authorize access="hasAnyRole('HO_MARKETING', 'HO_RETAIL', 'SUPERADMIN_DIT')">
                        <a href="${pageContext.request.contextPath}/admin/leads" class="nav-link ${activeTab == 'leads' ? 'active' : ''}" id="nav-link-leads">Customer Leads</a>
                    </sec:authorize>
                    <sec:authorize access="hasRole('SUPERADMIN_DIT')">
                        <a href="${pageContext.request.contextPath}/admin/audit-logs" class="nav-link ${activeTab == 'audit' ? 'active' : ''}" id="nav-link-audit-logs">Logs</a>
                    </sec:authorize>
                </sec:authorize>
            </div>
        </div>
    </nav>
</header>
<jsp:include page="gate-modal.jsp"/>
