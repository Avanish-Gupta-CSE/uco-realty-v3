/**
 * UCO Bank Realty - Core Application JavaScript
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 * Framework: Bootstrap 5.3.2 + Standard Vanilla JS
 */

document.addEventListener("DOMContentLoaded", function () {
    initGlobalConfirmHandlers();
    initErrorAlertScroll();
});

/**
 * Global Confirm Handlers (data-confirm="Message")
 */
function initGlobalConfirmHandlers() {
    document.querySelectorAll("[data-confirm]").forEach(function (el) {
        el.addEventListener("click", function (e) {
            var msg = el.getAttribute("data-confirm");
            if (msg && !confirm(msg)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });

    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (e) {
            var msg = form.getAttribute("data-confirm");
            if (msg && !confirm(msg)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
}

/**
 * Auto-scroll to #formErrorAlert if present
 */
function initErrorAlertScroll() {
    var alertEl = document.getElementById("formErrorAlert");
    if (alertEl) {
        alertEl.scrollIntoView({ behavior: "smooth", block: "center" });
    }
}

