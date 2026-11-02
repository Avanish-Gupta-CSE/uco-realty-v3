/**
 * UCO Bank Realty - Admin Project Directory JavaScript
 * Checker decision reviews and inventory adjustments with Bootstrap 5 modals.
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 */

document.addEventListener("DOMContentLoaded", function () {
    initCheckerReviewModal();
    initReduceUnitsModal();
});

/**
 * Checker Decision Modal
 */
function initCheckerReviewModal() {
    var modalEl = document.getElementById("decisionModal");
    var form = document.getElementById("decisionForm");
    var nameSpan = document.getElementById("dialogProjectName");
    var select = document.getElementById("decisionSelect");
    var comment = document.getElementById("checkerComment");
    var reqText = document.getElementById("commentReqText");

    if (!modalEl || !form) return;

    // Review button clicks
    document.querySelectorAll(".btn-review-project").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var projectId = btn.getAttribute("data-project-id");
            var projectName = btn.getAttribute("data-project-name");
            var context = btn.getAttribute("data-context-path") || "";

            if (nameSpan) nameSpan.innerText = projectName;
            form.action = context + "/admin/projects/" + encodeURIComponent(projectId) + "/decide";

            if (select) select.value = "approve";
            if (comment) {
                comment.value = "";
                comment.required = false;
            }
            if (reqText) reqText.classList.add("d-none");

            if (typeof bootstrap !== "undefined") {
                var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });

    // Toggle comment required when decision changes
    if (select) {
        select.addEventListener("change", function () {
            var isApprove = select.value === "approve";
            if (reqText) {
                if (isApprove) {
                    reqText.classList.add("d-none");
                } else {
                    reqText.classList.remove("d-none");
                }
            }
            if (comment) {
                comment.required = !isApprove;
            }
        });
    }
}

/**
 * Reduce Units Modal (HO Retail)
 */
function initReduceUnitsModal() {
    var modalEl = document.getElementById("reduceModal");
    var form = document.getElementById("reduceForm");
    var nameSpan = document.getElementById("reduceProjectName");
    var unitsSpan = document.getElementById("currentUnits");
    var unitsInput = document.getElementById("reduceUnits");
    var reasonInput = document.getElementById("reduceReason");

    if (!modalEl || !form) return;

    document.querySelectorAll(".btn-reduce-units").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var projectId = btn.getAttribute("data-project-id");
            var projectName = btn.getAttribute("data-project-name");
            var current = parseInt(btn.getAttribute("data-current-units") || "0", 10);
            var context = btn.getAttribute("data-context-path") || "";

            if (nameSpan) nameSpan.innerText = projectName;
            if (unitsSpan) unitsSpan.innerText = current;

            if (unitsInput) {
                unitsInput.value = "";
                unitsInput.max = current;
            }
            if (reasonInput) reasonInput.value = "";

            form.action = context + "/admin/projects/" + encodeURIComponent(projectId) + "/reduce-units";

            if (typeof bootstrap !== "undefined") {
                var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });
}

