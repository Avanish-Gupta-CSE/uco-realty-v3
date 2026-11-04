/**
 * Checker review page: a remark is required to return or reject a request (the server enforces it too).
 */
document.addEventListener("DOMContentLoaded", function () {
    var select = document.getElementById("changeDecisionSelect");
    var comment = document.getElementById("changeDecisionComment");
    if (!select || !comment) return;
    function sync() { comment.required = select.value !== "approve"; }
    select.addEventListener("change", sync);
    sync();
});
