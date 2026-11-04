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
    <link rel="stylesheet" id="uco-theme-css" href="${pageContext.request.contextPath}/assets/css/style.css">
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

            <!-- Customer Portal Contact Pill -->
            <div class="user-menu" id="customer-support-pill">
                <a href="https://www.uco.bank.in/home-loan" class="btn btn-outline-light btn-sm" id="btn-header-apply-loan">
                    Apply for UCO Home Loan
                </a>
            </div>
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
<jsp:include page="gate-modal.jsp"/>

