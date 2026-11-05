<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Staff Login - UCO Bank Realty Portal</title>
    <link rel="icon" type="image/png" id="favicon-link" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" id="bootstrap-css" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" id="uco-theme-css" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="login-bg" id="login-page-body">

<div class="login-card-wide" id="login-card-container">
    <div class="login-logo" id="login-logo-wrap">
        <a href="${pageContext.request.contextPath}/" id="login-logo-link">
            <img src="${pageContext.request.contextPath}/assets/images/uco_bank_logo.png" alt="UCO Bank Logo" id="login-logo-img">
        </a>
    </div>
    <h2 id="login-title">UCO Bank Realty Portal</h2>
    <p id="login-subtitle">Internal Employee Single Sign-On (SSO) Portal</p>

    <c:if test="${param.error == 'invalid_employee'}">
        <div class="alert alert-danger py-2 small mb-3" id="login-invalid-emp-alert">
            Employee ID not found in UCO Bank HRMS database or account is inactive.
        </div>
    </c:if>

    <c:if test="${param.error == 'bad_credentials'}">
        <div class="alert alert-danger py-2 small mb-3" id="login-bad-credentials-alert">
            Wrong employee number or password.
        </div>
    </c:if>

    <c:if test="${param.error != null && param.error != 'invalid_employee' && param.error != 'bad_credentials'}">
        <div class="alert alert-danger py-2 small mb-3" id="login-error-alert">
            Single Sign-On could not be completed. Please sign in again through the UCO Bank portal or with your Employee ID.
        </div>
    </c:if>

    <c:if test="${param.logout != null}">
        <div class="alert alert-success py-2 small mb-3" id="login-logout-alert">
            You have been securely logged out from the portal.
        </div>
    </c:if>

    <c:if test="${!devLogin && empty demoUsers}">
        <div class="alert alert-info py-3 small" id="alert-sso-portal-only">
            <strong>Single Sign-On required.</strong> Please open the Realty portal from the UCO Bank intranet
            portal. Direct sign-in on this page is not available.
        </div>
    </c:if>

    <c:if test="${not empty demoUsers}">
    <!-- UAT demo login: employee number + password of a fixed demo user (class DemoUserDirectory) -->
    <div id="pane-demo" class="tab-pane-content">
        <div class="alert alert-warning py-2 small mb-3" id="alert-demo-mode">
            <strong>UAT demo.</strong> Sign in with one of the demo users below. Real sign-in is through the UCO Bank Single Sign-On.
        </div>
        <form action="${pageContext.request.contextPath}/demo-login" method="post" id="demoLoginForm">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="mb-3">
                <label for="empId" class="form-label fw-semibold small" id="label-emp-id">Employee ID</label>
                <input type="text" id="empId" name="empId" class="form-control" placeholder="e.g. UAT003" required autofocus autocomplete="off">
            </div>
            <div class="mb-3">
                <label for="demoPassword" class="form-label fw-semibold small" id="label-demo-password">Password</label>
                <input type="password" id="demoPassword" name="password" class="form-control" required autocomplete="off">
            </div>
            <div class="mb-3" id="demo-users-wrap">
                <label class="form-label text-muted small fw-semibold" id="label-demo-users">Demo users (click to fill the Employee ID):</label>
                <div class="role-pills-container" id="pills-container">
                    <c:forEach var="u" items="${demoUsers}">
                        <button type="button" class="role-pill-btn" data-emp-id="<c:out value='${u.empNo}'/>" id="pill-<c:out value='${u.empNo}'/>">
                            <c:out value="${u.empNo}"/> &middot; <c:out value="${u.roleLabel}"/>
                        </button>
                    </c:forEach>
                </div>
            </div>
            <button type="submit" class="btn btn-primary w-100 py-2 mt-2" id="btn-demo-login">Sign in &rarr;</button>
        </form>
    </div>
    </c:if>

    <c:if test="${devLogin && empty demoUsers}">
    <!-- Employee SSO (OTP Flow) -->
    <div id="pane-sso" class="tab-pane-content">
        <form action="${pageContext.request.contextPath}/sso/generate-otp" method="post" id="ssoOtpForm">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="mb-3">
                <label for="empId" class="form-label fw-semibold small" id="label-emp-id">
                    Employee ID (Emp No)
                </label>
                <input type="text" id="empId" name="empId" class="form-control" placeholder="Employee number" required autofocus autocomplete="off">
                <div class="form-text small text-muted" id="emp-id-hint">
                    Enter your official 6-digit UCO Bank Employee Number.
                </div>
            </div>

            <c:if test="${testOtpMissing}">
            <div class="alert alert-warning py-2 small mb-3" id="alert-test-otp-missing">
                The test OTP is not configured on this server (setting <code>UCO_SSO_TEST_OTP</code>), so sign-in cannot be completed.
            </div>
            </c:if>

            <div class="alert alert-info py-2 small mb-3" id="alert-sso-info">
                <strong>HRMS Single Sign-On:</strong> Roles (Maker/Checker/Admin) are automatically derived from HRMS SOL ID &amp; Cadre Scale. <c:if test="${localMode}">Laptop test: the employees are in <code>db_migrations/05_LOCAL_TEST_EMPLOYEES.sql</code>; the OTP is the value of <code>uco.sso.test-otp</code>.</c:if><c:if test="${!localMode}">Enter the OTP given to you by the test administrator.</c:if>
            </div>

            <button type="submit" class="btn btn-primary w-100 py-2 mt-2" id="btn-sso-otp">
                Generate OTP &amp; Continue &rarr;
            </button>
        </form>
    </div>

    </c:if>

    <div class="mt-4 text-center" id="wrap-back-catalog">
        <a href="${pageContext.request.contextPath}/projects" class="login-back-link" id="link-back-catalog">
            &larr; Back to Public Project Catalog
        </a>
    </div>
</div>

<script id="bootstrap-bundle-js" src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script id="sso-login-js" src="${pageContext.request.contextPath}/assets/js/sso-login.js"></script>
</body>
</html>
