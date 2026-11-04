document.addEventListener('DOMContentLoaded', function () {
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
