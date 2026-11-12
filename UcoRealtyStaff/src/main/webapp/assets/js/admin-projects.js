/**
 * UCO Bank Realty - Admin Project Directory JavaScript
 * Unit-reduction and deletion requests (Bootstrap 5 modals). The checker reviews on its own page (change-review.jsp).
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 */

document.addEventListener("DOMContentLoaded", function () {
    initReduceUnitsModal();
    initDeleteModal();
});

/**
 * Reduce Units Modal (maker: raises an edit that a checker must approve)
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

/**
 * Delete Project Modal (maker: asks the checker to delete an approved project)
 */
function initDeleteModal() {
    var modalEl = document.getElementById("deleteModal");
    var form = document.getElementById("deleteForm");
    var nameSpan = document.getElementById("deleteProjectName");
    var reasonInput = document.getElementById("deleteReason");

    if (!modalEl || !form) return;

    document.querySelectorAll(".btn-delete-project").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var projectId = btn.getAttribute("data-project-id");
            var context = btn.getAttribute("data-context-path") || "";
            if (nameSpan) nameSpan.innerText = btn.getAttribute("data-project-name") || "";
            if (reasonInput) reasonInput.value = "";
            form.action = context + "/admin/projects/" + encodeURIComponent(projectId) + "/delete-request";
            if (typeof bootstrap !== "undefined") {
                bootstrap.Modal.getOrCreateInstance(modalEl).show();
            }
        });
    });
}
