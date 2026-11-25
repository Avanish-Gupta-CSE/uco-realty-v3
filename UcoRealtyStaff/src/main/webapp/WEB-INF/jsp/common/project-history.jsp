<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%-- Workflow and Audit History of one project (who did what, when). The page sets "history" (ProjectHistoryService.of). --%>
<c:if test="${not empty history}">
<section class="card-custom history-card mt-4" id="project-history-card">
    <h2 class="card-title-custom" id="project-history-heading">Workflow &amp; Audit History</h2>
    <div class="history-timeline" id="project-history-list">
        <c:forEach var="h" items="${history}" varStatus="st">
            <div class="history-item" id="history-item-${st.index}">
                <span class="history-dot" id="history-dot-${st.index}"></span>
                <div class="history-box">
                    <div class="history-head">
                        <span class="fw-bold">Status:</span> <span class="badge text-bg-${h.badge}" id="history-status-${st.index}"><c:out value="${h.label}"/></span>
                        <span class="history-time" id="history-time-${st.index}"><c:out value="${h.time}"/></span>
                    </div>
                    <div class="history-by" id="history-by-${st.index}">Action by: <strong><c:out value="${h.name}"/></strong><c:if test="${h.name != h.empId}"> (<c:out value="${h.empId}"/>)</c:if></div>
                    <c:if test="${not empty h.remarks}"><div class="history-remarks" id="history-remarks-${st.index}"><strong>Remarks:</strong> <c:out value="${h.remarks}"/></div></c:if>
                </div>
            </div>
        </c:forEach>
    </div>
</section>
</c:if>
