<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Security Authentication Notice - UCO Realty Intranet Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="error-layout">
  <div class="error-card">
    <div class="error-icon">&#x2715;</div>
    <h1 class="error-title">Security Authentication Notice</h1>
    <p class="error-desc">${errorMessage != null ? errorMessage : "Missing or invalid Single Sign-On parameters. Please sign in via the official bank portal or use direct staff authentication."}</p>
    <div class="error-hint">
      Please contact the SWDC Cell / DIT Central Administrator or access this system via the official UCO Bank intranet terminal portal.
    </div>
    <div class="error-system-code">
      System Code: 403 UNAUTHORIZED / SEC_ERR_SSO
    </div>
    <div class="mt-4 d-flex justify-content-center gap-3">
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary btn-sm">
        &larr; Go to Staff Login
      </a>
      <a href="${pageContext.request.contextPath}/projects" class="btn btn-secondary btn-sm">
        Browse Projects Catalog
      </a>
    </div>
  </div>
</body>
</html>
