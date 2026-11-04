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

    <c:if test="${param.error != null && param.error != 'invalid_employee'}">
        <div class="alert alert-danger py-2 small mb-3" id="login-error-alert">
            Single Sign-On could not be completed. Please sign in again through the UCO Bank portal or with your Employee ID.
        </div>
    </c:if>

    <c:if test="${param.logout != null}">
        <div class="alert alert-success py-2 small mb-3" id="login-logout-alert">
            You have been securely logged out from the portal.
        </div>
    </c:if>

    <c:if test="${!devLogin}">
        <div class="alert alert-info py-3 small" id="alert-sso-portal-only">
            <strong>Single Sign-On required.</strong> Please open the Realty portal from the UCO Bank intranet
            portal. Direct sign-in on this page is not available.
        </div>
    </c:if>

    <c:if test="${devLogin}">
    <!-- Employee SSO (OTP Flow) -->
    <div id="pane-sso" class="tab-pane-content">
        <form action="${pageContext.request.contextPath}/sso/generate-otp" method="post" id="ssoOtpForm">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="mb-3">
                <label for="empId" class="form-label fw-semibold small" id="label-emp-id">
                    Employee ID (Emp No)
                </label>
                <input type="text" id="empId" name="empId" class="form-control" placeholder="e.g. 100001, 200001" required autofocus autocomplete="off">
                <div class="form-text small text-muted" id="emp-id-hint">
                    Enter your official 6-digit UCO Bank Employee Number.
                </div>
            </div>

            <c:if test="${localMode}">
            <!-- Role Selector Helper Pills (Dev & Testing) -->
            <div class="mb-3" id="role-quick-select-wrap">
                <label class="form-label text-muted small fw-semibold" id="label-quick-test">
                    Quick Role Selector (Testing):
                </label>
                <div class="role-pills-container" id="pills-container">
                    <button type="button" class="role-pill-btn" data-emp-id="100001" id="pill-100001">
                        100001 (Superadmin DIT)
                    </button>
                    <button type="button" class="role-pill-btn" data-emp-id="200001" id="pill-200001">
                        200001 (Ajmer Maker)
                    </button>
                    <button type="button" class="role-pill-btn" data-emp-id="200002" id="pill-200002">
                        200002 (Ajmer Checker)
                    </button>
                    <button type="button" class="role-pill-btn" data-emp-id="300001" id="pill-300001">
                        300001 (Bhopal Maker)
                    </button>
                    <button type="button" class="role-pill-btn" data-emp-id="300002" id="pill-300002">
                        300002 (Bhopal Checker)
                    </button>
                    <button type="button" class="role-pill-btn" data-emp-id="400001" id="pill-400001">
                        400001 (HO Retail)
                    </button>
                </div>
            </div>

            </c:if>

            <div class="alert alert-info py-2 small mb-3" id="alert-sso-info">
                <strong>HRMS Single Sign-On:</strong> Roles (Maker/Checker/Admin) are automatically derived from HRMS SOL ID &amp; Cadre Scale. <c:if test="${localMode}">Local test OTP: <code>123456</code>.</c:if><c:if test="${!localMode}">Enter the OTP given to you by the test administrator.</c:if>
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
