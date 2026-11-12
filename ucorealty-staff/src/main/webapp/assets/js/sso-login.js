document.addEventListener('DOMContentLoaded', function () {
  // The notice (session expired, signed out, wrong employee id ...) comes from the address, e.g. /login?expired. Show it once:
  // remove the part after "?" so a reload (even a hard reload) opens the clean login page without the banner.
  if (window.location.search && window.history && window.history.replaceState) {
    window.history.replaceState(null, '', window.location.pathname);
  }

  // Role selector quick-fill pills
  const rolePills = document.querySelectorAll('.role-pill-btn');
  const empIdInput = document.getElementById('empId');

  rolePills.forEach(function (pill) {
    pill.addEventListener('click', function () {
      const empId = pill.getAttribute('data-emp-id');
      if (empIdInput && empId) {
        empIdInput.value = empId;
        empIdInput.focus();
      }
    });
  });
});
