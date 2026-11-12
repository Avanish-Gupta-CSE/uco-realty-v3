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
 * Global confirm box: any button or form with data-confirm="Message" asks first, in our own dialog (not the browser's popup).
 * Optional attributes: data-confirm-ok="Yes, withdraw" (button text), data-confirm-style="danger|success|primary" (button colour),
 * data-confirm-detail="#selectId" (adds the chosen option of that select, e.g. the checker's decision).
 */
function initGlobalConfirmHandlers() {
    var modalEl = document.getElementById("ucoConfirmModal");
    if (!modalEl || typeof bootstrap === "undefined") return;
    var modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    var yes = document.getElementById("ucoConfirmYes");
    var pending = null;     // { form, submitter } waiting for the user's answer
    var approved = false;   // set while we re-submit after "Yes", so the handler does not ask again

    function ask(el, form, submitter) {
        var detail = "";
        var sel = el.getAttribute("data-confirm-detail") ? document.querySelector(el.getAttribute("data-confirm-detail")) : null;
        if (sel && sel.selectedIndex >= 0) detail = sel.options[sel.selectedIndex].text;
        document.getElementById("ucoConfirmMessage").textContent = el.getAttribute("data-confirm");
        document.getElementById("ucoConfirmDetail").textContent = detail;
        yes.textContent = el.getAttribute("data-confirm-ok") || "Yes, continue";
        yes.className = "btn btn-sm btn-" + (el.getAttribute("data-confirm-style") || "primary");
        pending = { form: form, submitter: submitter };
        modal.show();
    }

    // for scripts: window.ucoConfirm("Message", "Yes, delete", "danger", function () { ... })
    window.ucoConfirm = function (message, okText, style, onYes) {
        document.getElementById("ucoConfirmMessage").textContent = message;
        document.getElementById("ucoConfirmDetail").textContent = "";
        yes.textContent = okText || "Yes, continue";
        yes.className = "btn btn-sm btn-" + (style || "primary");
        pending = { callback: onYes };
        modal.show();
    };

    yes.addEventListener("click", function () {
        var p = pending;
        pending = null;
        modal.hide();
        if (!p) return;
        if (p.callback) { p.callback(); return; }
        approved = true;
        if (p.submitter && p.submitter.form === p.form) p.form.requestSubmit(p.submitter);   // keeps the button's name/value (e.g. action=submit)
        else p.form.requestSubmit();
        approved = false;
    });
    modalEl.addEventListener("hidden.bs.modal", function () { pending = null; });

    // a button with data-confirm: ask on click, but let the browser show its own "fill this field" hints first
    document.querySelectorAll("button[data-confirm], input[type=submit][data-confirm]").forEach(function (btn) {
        btn.addEventListener("click", function (e) {
            if (approved || !btn.form) return;
            if (!btn.formNoValidate && !btn.form.checkValidity()) return;
            e.preventDefault();
            ask(btn, btn.form, btn);
        });
    });
    // a form with data-confirm: ask when it is submitted
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (e) {
            if (approved) return;
            e.preventDefault();
            ask(form, form, e.submitter);
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

