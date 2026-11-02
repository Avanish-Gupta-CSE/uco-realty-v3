/**
 * UCO Bank Realty Portal - Audit Logs Script
 * Bank Compliance: Zero inline JS, all element access via explicit IDs.
 */
document.addEventListener('DOMContentLoaded', function() {
    var urlParams = new URLSearchParams(window.location.search);
    var activeTabParam = urlParams.get('tab');

    var activityTabBtn = document.getElementById('tab-btn-activity');
    var loginTabBtn = document.getElementById('tab-btn-login');

    if (activeTabParam === 'login' && loginTabBtn && window.bootstrap) {
        var tabInstance = bootstrap.Tab.getOrCreateInstance(loginTabBtn);
        if (tabInstance) {
            tabInstance.show();
        }
    }

    if (activityTabBtn && loginTabBtn) {
        activityTabBtn.addEventListener('shown.bs.tab', function() {
            updatePaginationTabParams('activity');
        });

        loginTabBtn.addEventListener('shown.bs.tab', function() {
            updatePaginationTabParams('login');
        });
    }

    var currentActive = (activeTabParam === 'login') ? 'login' : 'activity';
    updatePaginationTabParams(currentActive);

    function updatePaginationTabParams(tabName) {
        var pageLinks = document.querySelectorAll('.pagination-custom .page-link');
        for (var i = 0; i < pageLinks.length; i++) {
            var link = pageLinks[i];
            if (link.getAttribute('href') && link.getAttribute('href').indexOf('?') !== -1) {
                var url = new URL(link.href, window.location.origin);
                url.searchParams.set('tab', tabName);
                link.href = url.pathname + url.search;
            }
        }
    }
});

