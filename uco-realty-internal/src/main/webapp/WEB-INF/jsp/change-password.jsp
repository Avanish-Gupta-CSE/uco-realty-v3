<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

<c:set var="pageTitle" value="Change Password - UCO Realty" scope="request"/>
<jsp:include page="common/header.jsp"/>

<main class="container py-4" id="main-change-password">
    <div class="row justify-content-center" id="change-pass-row">
        <div class="col-md-7 col-lg-5">
            <div class="card-custom" id="card-change-password">
                <h2 class="card-title-custom" id="heading-change-password">Change Password</h2>

                <c:if test="${message != null}">
                    <div class="alert alert-success py-2 small mb-3" id="alert-change-success">${message}</div>
                </c:if>
                <c:if test="${error != null}">
                    <div class="alert alert-danger py-2 small mb-3" id="alert-change-error">${error}</div>
                </c:if>

                <div id="passwordMismatchAlert" class="alert alert-danger py-2 small mb-3 d-none">
                    New Password and Confirm Password do not match.
                </div>

                <form id="changePasswordForm" action="${pageContext.request.contextPath}/admin/change-password" method="post">
                    <div class="mb-3">
                        <label for="currentPassword" class="form-label fw-semibold small">Current Password *</label>
                        <input type="password" id="currentPassword" name="currentPassword" class="form-control" required autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="newPassword" class="form-label fw-semibold small">New Password * (min 8 chars, 1 letter, 1 digit)</label>
                        <input type="password" id="newPassword" name="newPassword" class="form-control" required minlength="8">
                    </div>

                    <div class="mb-3">
                        <label for="confirmPassword" class="form-label fw-semibold small">Confirm New Password *</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" required minlength="8">
                    </div>

                    <div class="d-flex gap-2 mt-4 pt-2 border-top" id="change-pass-actions">
                        <button type="submit" class="btn btn-primary flex-fill" id="btn-submit-change-password">Update Password</button>
                        <a href="${pageContext.request.contextPath}/admin/projects" class="btn btn-secondary" id="btn-cancel-change-password">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</main>

<!-- Password Validation Script -->
<script id="change-password-js" src="${pageContext.request.contextPath}/assets/js/change-password.js"></script>

<jsp:include page="common/footer.jsp"/>
