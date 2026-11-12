<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<footer class="footer" id="app-footer">
    <div class="container text-center">
        <p class="mb-0" id="copyright-text">&copy; <%= java.time.Year.now() %> UCO Bank. All rights reserved. | Approved Housing Projects Portal</p>
        <p class="mb-0 mt-1" id="developed-by-text">Developed By UCO Bank - Department of Information Technology</p>
    </div>
</footer>

<!-- Confirm box used instead of the browser's own popup (see initGlobalConfirmHandlers in app.js) -->
<div class="modal fade" id="ucoConfirmModal" tabindex="-1" aria-labelledby="ucoConfirmTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-primary" id="ucoConfirmTitle">Please confirm</h5>
                <button type="button" class="btn-close" id="ucoConfirmClose" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <p class="mb-1" id="ucoConfirmMessage"></p>
                <p class="mb-0 fw-semibold" id="ucoConfirmDetail"></p>
            </div>
            <div class="modal-footer border-top bg-light">
                <button type="button" class="btn btn-secondary btn-sm" id="ucoConfirmNo" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary btn-sm" id="ucoConfirmYes">Yes, continue</button>
            </div>
        </div>
    </div>
</div>

<!-- Bootstrap 5.3.2 JS Bundle (Bank Standard) -->
<script id="bootstrap-bundle-js" src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<!-- UCO Bank Application Script -->
<script id="uco-app-js" src="${pageContext.request.contextPath}/assets/js/app.js?v=${assetVersion}"></script>
</body>
</html>
