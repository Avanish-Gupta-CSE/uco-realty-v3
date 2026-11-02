document.addEventListener('DOMContentLoaded', function () {
  // Role selector quick-fill pills
  const rolePills = document.querySelectorAll('.role-pill-btn');
  const empIdInput = document.getElementById('empId');
  const usernameInput = document.getElementById('username');

  if (rolePills.length > 0) {
    rolePills.forEach(function (pill) {
      pill.addEventListener('click', function () {
        const empId = pill.getAttribute('data-emp-id');
        if (empIdInput && empId) {
          empIdInput.value = empId;
          empIdInput.focus();
        }
        if (usernameInput && empId) {
          usernameInput.value = empId;
        }
      });
    });
  }

  // Tab switching between SSO OTP and Direct Password
  const tabSso = document.getElementById('tab-btn-sso');
  const tabPassword = document.getElementById('tab-btn-password');
  const paneSso = document.getElementById('pane-sso');
  const panePassword = document.getElementById('pane-password');

  if (tabSso && tabPassword && paneSso && panePassword) {
    tabSso.addEventListener('click', function () {
      tabSso.classList.add('active');
      tabPassword.classList.remove('active');
      paneSso.classList.remove('d-none');
      panePassword.classList.add('d-none');
      if (empIdInput) empIdInput.focus();
    });

    tabPassword.addEventListener('click', function () {
      tabPassword.classList.add('active');
      tabSso.classList.remove('active');
      panePassword.classList.remove('d-none');
      paneSso.classList.add('d-none');
      if (usernameInput) usernameInput.focus();
    });
  }
});

