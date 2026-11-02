document.addEventListener('DOMContentLoaded', function () {
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

