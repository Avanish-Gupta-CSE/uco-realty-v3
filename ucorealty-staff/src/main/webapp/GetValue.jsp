<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // UCO Bank Intranet SSO Entry Bridge
    // Compatible with legacy bank portal redirects targeting /GetValue.jsp
    String token = request.getParameter("Value");
    if (token == null || token.isEmpty()) {
        token = request.getParameter("value");
    }
    if (token != null && !token.isEmpty()) {
        request.getRequestDispatcher("/entry").forward(request, response);
    } else {
        response.sendRedirect(request.getContextPath() + "/login");
    }
%>

