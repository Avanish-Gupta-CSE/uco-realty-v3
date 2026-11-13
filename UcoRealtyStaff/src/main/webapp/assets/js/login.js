/**
 * The two sign-in pages: login.jsp (shows its notice once) and otp.jsp (the six one-time-password boxes).
 * The OTP part does nothing on a page without the boxes, so both pages load this one file.
 */
document.addEventListener('DOMContentLoaded', function () {
  // The notice (session expired, signed out, wrong employee id, wrong OTP ...) comes from the address, e.g. /login?expired.
  // Show it once: remove the part after "?" so a reload (even a hard reload) opens the clean page without the banner.
  if (window.location.search && window.history && window.history.replaceState) {
    window.history.replaceState(null, '', window.location.pathname);
  }

  // ---- otp.jsp: six boxes that make one OTP
  const otpFields = document.querySelectorAll('.otp-box');
  const hiddenOtpInput = document.getElementById('hidden-otp-input');
  const otpForm = document.getElementById('otpForm');

  if (otpFields.length > 0) {
    function combineOtp() {
      let combined = '';
      otpFields.forEach(function (field) {
        combined += field.value;
      });
      if (hiddenOtpInput) {
        hiddenOtpInput.value = combined;
      }
    }

    otpFields.forEach(function (field, index) {
      // Forward focus transition on character input
      field.addEventListener('input', function () {
        if (field.value.length === 1 && index < otpFields.length - 1) {
          otpFields[index + 1].focus();
        }
        combineOtp();
      });

      // Backward focus transition on Backspace
      field.addEventListener('keydown', function (e) {
        if (e.key === 'Backspace' && field.value.length === 0 && index > 0) {
          otpFields[index - 1].focus();
        }
      });

      // Allow pasting 6-digit OTP
      field.addEventListener('paste', function (e) {
        e.preventDefault();
        const pasted = (e.clipboardData || window.clipboardData).getData('text').trim();
        if (pasted && /^\d{6}$/.test(pasted)) {
          for (let i = 0; i < 6; i++) {
            if (otpFields[i]) {
              otpFields[i].value = pasted.charAt(i);
            }
          }
          combineOtp();
          otpFields[5].focus();
        }
      });
    });

    if (otpForm) {
      otpForm.addEventListener('submit', function () {
        combineOtp();
      });
    }

    // Auto-focus first box
    if (otpFields[0]) {
      otpFields[0].focus();
    }
  }
});
