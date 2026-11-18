<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<footer class="footer" id="app-footer">
    <div class="container text-center">
        <p class="mb-0" id="copyright-text">&copy; <jsp:useBean id="today" class="java.util.Date"/><fmt:formatDate value="${today}" pattern="yyyy"/> UCO Bank. All rights reserved. | Approved Housing Projects Portal</p>
        <p class="mb-0 mt-1" id="developed-by-text">Developed By UCO Bank - Department of Information Technology</p>
    </div>
</footer>

<%-- Bootstrap 5.3.2 JS Bundle (Bank Standard) --%>
<script id="bootstrap-bundle-js" src="${pageContext.request.contextPath}/assets/js/bootstrap.bundle.min.js"></script>
<%-- UCO Bank Application Script --%>
<script id="uco-app-js" src="${pageContext.request.contextPath}/assets/js/app.js?v=20261106a"></script>
</body>
</html>
