/**
 * UCO Bank Realty - Self Password Change JavaScript
 * Validates new password and confirmation match.
 * Compliance: Zero inline JS (Strict CSP compliance for bank audits)
 */

document.addEventListener("DOMContentLoaded", function () {
    var form = document.getElementById("changePasswordForm");
    if (!form) return;

    form.addEventListener("submit", function (e) {
        var p1 = document.getElementById("newPassword");
        var p2 = document.getElementById("confirmPassword");
        var errorBanner = document.getElementById("passwordMismatchAlert");

        if (p1 && p2 && p1.value !== p2.value) {
            e.preventDefault();
            if (errorBanner) {
                errorBanner.classList.remove("d-none");
            } else {
                alert("New Password and Confirm Password do not match.");
            }
            p2.focus();
        }
    });
});

