<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><c:out value="${noticeTitle}"/> - UCO Bank Realty Portal</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=20261130a">
</head>
<body class="error-layout">
  <div class="error-card" id="notice-card-${noticeKind}">
    <img src="${pageContext.request.contextPath}/assets/images/uco_bank_logo.png" alt="UCO Bank" class="error-logo" id="notice-logo">
    <div class="error-icon ${noticeKind == 'signed-out' ? 'error-icon-success' : 'error-icon-info'}" id="notice-icon">${noticeKind == 'signed-out' ? '&#10003;' : '!'}</div>
    <h1 class="error-title" id="notice-title"><c:out value="${noticeTitle}"/></h1>
    <p class="error-desc" id="notice-message"><c:out value="${noticeMessage}"/></p>
  </div>
</body>
</html>
