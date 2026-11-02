<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OTP Verification - UCO Bank Realty Portal</title>
    <link rel="icon" type="image/png" id="favicon-link" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="stylesheet" id="bootstrap-css" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" id="uco-theme-css" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="login-bg" id="otp-page-body">

<div class="login-card-wide" id="otp-card-container">
    <div class="login-logo" id="otp-logo-wrap">
        <a href="${pageContext.request.contextPath}/" id="otp-logo-link">
            <img src="${pageContext.request.contextPath}/assets/images/uco_bank_logo.png" alt="UCO Bank Logo" id="otp-logo-img">
        </a>
    </div>

    <h2 id="otp-title">Security Verification</h2>
    <p id="otp-subtitle">Enter One-Time Password sent to your registered UCO mobile</p>

    <!-- Verified Employee Card -->
    <div class="card bg-light border-0 mb-3" id="emp-info-card">
        <div class="card-body p-3 small">
            <div class="row g-1">
                <div class="col-5 text-muted fw-semibold">Employee ID:</div>
                <div class="col-7 fw-bold text-dark" id="otp-val-empid">${empId}</div>

                <div class="col-5 text-muted fw-semibold">Staff Name:</div>
                <div class="col-7 fw-bold text-primary" id="otp-val-name">${empName}</div>

                <div class="col-5 text-muted fw-semibold">Posting SOL:</div>
                <div class="col-7 text-dark" id="otp-val-sol">${empSol}</div>

                <div class="col-5 text-muted fw-semibold">Portal Role:</div>
                <div class="col-7" id="otp-val-role">
                    <span class="sso-badge">${empRole}</span>
                </div>

                <div class="col-5 text-muted fw-semibold">Mobile No:</div>
                <div class="col-7 text-muted" id="otp-val-mobile">${empMobile}</div>
            </div>
        </div>
    </div>

    <c:if test="${param.error == 'otp'}">
        <div class="alert alert-danger py-2 small mb-3 text-start" id="otp-error-alert">
            Invalid or expired OTP. Please check the code and try again. (Local Dev Test OTP: <code>123456</code>)
        </div>
    </c:if>

    <div class="alert alert-warning py-2 small mb-3 text-start" id="otp-dev-hint">
        <strong>Security Check:</strong> Simulated SMS OTP sent. Check your server console/logs for code (Local mode: <code>123456</code>).
    </div>

    <form action="${pageContext.request.contextPath}/otp-verify" method="post" id="otpForm">
        <!-- Hidden combined OTP value -->
        <input type="hidden" id="hidden-otp-input" name="otp" required>

        <!-- 6 Individual Box Inputs -->
        <div class="otp-input-group" id="otp-input-boxes">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" autofocus id="otp-box-1">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" id="otp-box-2">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" id="otp-box-3">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" id="otp-box-4">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" id="otp-box-5">
            <input type="text" class="otp-box form-control" maxlength="1" required autocomplete="off" id="otp-box-6">
        </div>

        <button type="submit" class="btn btn-primary w-100 py-2 fw-semibold mb-3" id="btn-verify-otp">
            Verify OTP &amp; Establish Session
        </button>

        <div class="text-center" id="otp-back-wrap">
            <a href="${pageContext.request.contextPath}/login" class="text-decoration-none text-muted small" id="link-cancel-otp">
                &larr; Cancel and change Employee ID
            </a>
        </div>
    </form>
</div>

<script id="bootstrap-bundle-js" src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<script id="otp-js" src="${pageContext.request.contextPath}/assets/js/otp.js"></script>
</body>
</html>

