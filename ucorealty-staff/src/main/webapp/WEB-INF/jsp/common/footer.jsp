<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<footer class="footer" id="app-footer">
    <div class="container text-center">
        <p class="mb-0" id="copyright-text">&copy; <%= java.time.Year.now() %> UCO Bank. All rights reserved. | Approved Housing Projects Portal</p>
    </div>
</footer>

<!-- Bootstrap 5.3.2 JS Bundle (Bank Standard) -->
<script id="bootstrap-bundle-js" src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<!-- UCO Bank Application Script -->
<script id="uco-app-js" src="${pageContext.request.contextPath}/assets/js/app.js?v=${assetVersion}"></script>
</body>
</html>
