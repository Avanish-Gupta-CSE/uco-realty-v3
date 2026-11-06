/**
 * UCO Bank Realty - Core Application JavaScript
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 * Framework: Bootstrap 5.3.2 + Standard Vanilla JS
 */

document.addEventListener("DOMContentLoaded", function () {
    initArrivalGateModal();
    initLoanApplyTriggers();
    initGlobalConfirmHandlers();
    initErrorAlertScroll();
});

/**
 * Arrival Gate & Customer Lead Capture Modal
 */
function showLeadError(text) {
    var box = document.getElementById("leadFormError");
    if (box) {
        box.textContent = text;
        box.classList.remove("d-none");
    }
}

function initArrivalGateModal() {
    var modalEl = document.getElementById("customerLeadModal");
    if (!modalEl) return;

    var leadForm = document.getElementById("leadCaptureForm");
    var skipBtn = document.getElementById("skipLeadBtn");
    var nameInput = document.getElementById("custName");
    var phoneInput = document.getElementById("custPhone");

    // The app may be served under a context path (external Tomcat: /ucorealty-staff/): judge the page by the path AFTER it,
    // so the context name itself can never be mistaken for "/admin", "/login" or "/projects".
    var ctx = modalEl.getAttribute("data-context-path") || "";
    var appPath = window.location.pathname.indexOf(ctx) === 0 ? window.location.pathname.substring(ctx.length) : window.location.pathname;
    if (appPath === "") appPath = "/";

    var isPublicPage = appPath.indexOf("/admin") !== 0 && appPath.indexOf("/login") !== 0;

    if (!isPublicPage) return;

    var storedName = sessionStorage.getItem("uco_cust_name");
    var storedPhone = sessionStorage.getItem("uco_cust_phone");
    var isDismissed = sessionStorage.getItem("uco_gate_dismissed");

    // Pre-fill fields if stored
    if (nameInput && storedName) nameInput.value = storedName;
    if (phoneInput && storedPhone) phoneInput.value = storedPhone;

    // Show on catalog landing if not previously saved or dismissed
    var isCatalog = appPath === "/" || appPath.indexOf("/projects") === 0;

    if (isCatalog && !storedName && !isDismissed && typeof bootstrap !== "undefined") {
        var bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
        bsModal.show();
    }

    // Skip & Explore handler
    if (skipBtn) {
        skipBtn.addEventListener("click", function () {
            sessionStorage.setItem("uco_gate_dismissed", "true");
            var bsModal = bootstrap.Modal.getInstance(modalEl);
            if (bsModal) bsModal.hide();
        });
    }

    // Form submission
    if (leadForm) {
        leadForm.addEventListener("submit", function (e) {
            e.preventDefault();

            var name = nameInput ? nameInput.value.trim() : "";
            var phone = phoneInput ? phoneInput.value.trim() : "";
            var projIdInput = document.getElementById("modalProjectId");
            var statusInput = document.getElementById("modalStatus");
            var projId = projIdInput ? projIdInput.value : "";
            var status = statusInput ? statusInput.value : "Visitor";

            if (!name || !phone || !/^[0-9]{10}$/.test(phone)) {
                showLeadError("Please enter your name and a 10-digit mobile number.");
                return;
            }

            var submitBtn = document.getElementById("submitLeadBtn");
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerText = "Saving...";
            }

            // Save in session
            sessionStorage.setItem("uco_cust_name", name);
            sessionStorage.setItem("uco_cust_phone", phone);
            sessionStorage.setItem("uco_gate_dismissed", "true");

            var contextPath = modalEl.getAttribute("data-context-path") || "";
            var formData = new URLSearchParams();
            formData.append("name", name);
            formData.append("phone", phone);
            if (projId) formData.append("projectId", projId);
            if (status) formData.append("status", status);

            fetch(contextPath + "/lead", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-Requested-With": "XMLHttpRequest"
                },
                body: formData.toString()
            })
            .then(function (res) {
                if (!res.ok) {
                    // The details were refused: say why and let the customer correct them
                    sessionStorage.removeItem("uco_cust_name");
                    sessionStorage.removeItem("uco_cust_phone");
                    sessionStorage.removeItem("uco_gate_dismissed");
                    return res.json().catch(function () { return {}; }).then(function (body) {
                        showLeadError(body.error || "Please check your details and try again.");
                        if (submitBtn) {
                            submitBtn.disabled = false;
                            submitBtn.innerText = "Continue";
                        }
                    });
                }
                var bsModal = bootstrap.Modal.getInstance(modalEl);
                if (bsModal) bsModal.hide();

                if (status === "Showed interest in Home Loan") {
                    window.location.href = "https://www.uco.bank.in/home-loan";
                } else {
                    window.location.reload();
                }
            })
            .catch(function () {
                var bsModal = bootstrap.Modal.getInstance(modalEl);
                if (bsModal) bsModal.hide();
                if (status === "Showed interest in Home Loan") {
                    window.location.href = "https://www.uco.bank.in/home-loan";
                } else {
                    window.location.reload();
                }
            });
        });
    }
}

/**
 * Apply for Home Loan triggers on public project pages
 */
function initLoanApplyTriggers() {
    var buttons = document.querySelectorAll('[data-action="apply-loan"]');
    var modalEl = document.getElementById("customerLeadModal");

    buttons.forEach(function (btn) {
        btn.addEventListener("click", function () {
            var projectId = btn.getAttribute("data-project-id") || "";
            var storedName = sessionStorage.getItem("uco_cust_name");
            var storedPhone = sessionStorage.getItem("uco_cust_phone");

            if (modalEl && typeof bootstrap !== "undefined") {
                var titleEl = document.getElementById("leadModalTitle");
                var subEl = document.getElementById("leadModalSubtitle");
                var projIdInput = document.getElementById("modalProjectId");
                var statusInput = document.getElementById("modalStatus");
                var skipBtn = document.getElementById("skipLeadBtn");
                var submitBtn = document.getElementById("submitLeadBtn");
                var nameInput = document.getElementById("custName");
                var phoneInput = document.getElementById("custPhone");

                if (titleEl) titleEl.innerText = "Apply for UCO Home Loan";
                if (subEl) subEl.innerText = "Please confirm your contact details to connect with a UCO Bank loan representative.";
                if (projIdInput) projIdInput.value = projectId;
                if (statusInput) statusInput.value = "Showed interest in Home Loan";
                if (nameInput && storedName) nameInput.value = storedName;
                if (phoneInput && storedPhone) phoneInput.value = storedPhone;
                if (skipBtn) skipBtn.style.display = "none";
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerText = "Continue to Home Loan \u2192";
                }

                var bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
                bsModal.show();
            }
        });
    });
}

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

