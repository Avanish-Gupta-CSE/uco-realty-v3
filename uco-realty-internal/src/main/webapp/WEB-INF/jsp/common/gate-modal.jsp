<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!-- Customer Lead Capture Modal (Arrival Gate & Apply for Home Loan) -->
<div class="modal fade" id="customerLeadModal" tabindex="-1" aria-labelledby="leadModalTitle" aria-hidden="true" data-context-path="${pageContext.request.contextPath}">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom">
                <h5 class="modal-title fw-bold text-primary" id="leadModalTitle">Welcome to UCO Bank Realty</h5>
                <button type="button" class="btn-close" id="btn-close-lead-modal" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <form id="leadCaptureForm">
                <div class="modal-body p-4">
                    <p id="leadModalSubtitle" class="text-muted small mb-3">
                        Explore approved housing projects. Please share your details for quick home loan assistance, or continue browsing.
                    </p>

                    <input type="hidden" id="modalProjectId" name="projectId" value="">
                    <input type="hidden" id="modalStatus" name="status" value="Visitor">

                    <div class="mb-3">
                        <label for="custName" class="form-label fw-semibold small">Your Full Name *</label>
                        <input type="text" id="custName" name="name" class="form-control" required placeholder="e.g. Rahul Sharma" autocomplete="name">
                    </div>

                    <div class="mb-3">
                        <label for="custPhone" class="form-label fw-semibold small">Mobile Number *</label>
                        <input type="tel" id="custPhone" name="phone" class="form-control" pattern="[0-9]{10}" maxlength="10" required placeholder="10-digit mobile number" autocomplete="tel">
                        <div id="phoneHelpText" class="form-text small">We will never spam or share your number.</div>
                    </div>
                </div>

                <div class="modal-footer border-top bg-light">
                    <button type="button" id="skipLeadBtn" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Skip &amp; Explore</button>
                    <button type="submit" id="submitLeadBtn" class="btn btn-primary btn-sm">Continue</button>
                </div>
            </form>
        </div>
    </div>
</div>
