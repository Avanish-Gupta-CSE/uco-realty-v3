<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Notice - UCO Bank Realty Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=20261106a">
</head>
<body class="error-layout">
  <div class="error-card">
    <div class="error-icon error-icon-info" id="error-icon">!</div>
    <h1 class="error-title" id="error-title">We could not open this page</h1>
    <p class="error-desc" id="error-message"><c:out value="${errorMessage != null ? errorMessage : 'Please open the Realty portal from the UCO Bank intranet portal.'}"/></p>
    <div class="mt-4 d-flex justify-content-center gap-3">
      <a href="${pageContext.request.contextPath}/login" class="btn btn-primary btn-sm">
        Go to Staff Login
      </a>
      <a href="${pageContext.request.contextPath}/projects" class="btn btn-secondary btn-sm">
        Browse Projects
      </a>
    </div>
  </div>
</body>
</html>
