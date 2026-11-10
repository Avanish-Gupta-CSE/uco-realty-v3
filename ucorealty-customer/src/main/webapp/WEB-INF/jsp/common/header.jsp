<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
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
    <link rel="stylesheet" id="uco-theme-css" href="${pageContext.request.contextPath}/assets/css/style.css?v=20261106a">
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

            <%-- The top-right "Apply for UCO Home Loan" button was removed on purpose: customers apply from a project page
                 (button goes straight to the bank's Home Loan application, see public-detail.jsp). --%>
        </div>
    </div>

    <nav class="navbar navbar-expand navbar-custom" id="main-navigation">
        <div class="container">
            <div class="navbar-nav" id="main-navbar-nav">
                <a href="${pageContext.request.contextPath}/projects" class="nav-link active" id="nav-link-customer-portal">
                    Approved Projects Catalog
                </a>
            </div>
        </div>
    </nav>
</header>
<%-- The lead pop-up (gate-modal.jsp) is switched off for now: the bank's Home Loan page captures the customer's details itself.
     To bring it back, restore the line  <jsp:include page="gate-modal.jsp"/>  here. --%>

