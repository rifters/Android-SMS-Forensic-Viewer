/**
 * thread.js — JavaScript for thread_<id>.html pages
 *
 * On page load: scroll to the message identified by the URL fragment (#msg_<id>)
 * and apply the 'highlight' class so it stands out visually.
 */
(function () {
    function highlightTarget() {
        const hash = window.location.hash;
        if (!hash) return;

        const targetEl = document.querySelector(hash);
        if (!targetEl) return;

        // Add highlight class for CSS animation
        targetEl.classList.add('highlight');

        // Scroll into view with a small delay to let the page settle
        setTimeout(() => {
            targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 150);

        // Remove highlight class after animation completes
        setTimeout(() => {
            targetEl.classList.remove('highlight');
        }, 4000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', highlightTarget);
    } else {
        highlightTarget();
    }
})();
