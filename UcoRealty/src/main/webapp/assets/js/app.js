/**
 * UCO Bank Realty - Core Application JavaScript
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 * Framework: Bootstrap 5.3.2 + Standard Vanilla JS
 */

document.addEventListener("DOMContentLoaded", function () {
    initGlobalConfirmHandlers();
    initErrorAlertScroll();
    initPhotoCarousel();
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


/**
 * Project photo carousel: show "2 / 5" and, for visitors whose device asks for reduced motion, do not slide on its own.
 */
function initPhotoCarousel() {
    var el = document.getElementById("project-photo-carousel");
    if (!el) return;
    var counter = document.getElementById("gallery-counter");
    var total = el.querySelectorAll(".carousel-item").length;
    el.addEventListener("slid.bs.carousel", function (e) {
        if (counter) counter.textContent = (e.to + 1) + " / " + total;
    });
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches && window.bootstrap) {
        window.bootstrap.Carousel.getOrCreateInstance(el, { ride: false, interval: false }).pause();
    }
}
