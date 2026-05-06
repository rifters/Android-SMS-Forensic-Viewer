/**
 * thread.js — JavaScript for thread_<id>.html pages
 *
 * On page load:
 *   1. colorizeParticipants() — assigns consistent sender colors to group messages
 *   2. highlightTarget()      — scrolls to and highlights the message from #msg_<id>
 */
(function () {

    /** Name used for the device owner's outgoing messages — never colorized. */
    const SELF_SENDER_NAME = 'Me';

    /**
     * Assign a stable data-color attribute (1–8) to each incoming .message
     * based on the sender name, so each participant gets a distinct color.
     */
    function colorizeParticipants() {
        const colorMap = {};
        let   nextSlot = 1;

        document.querySelectorAll('#messages .message.incoming').forEach(msg => {
            const senderEl = msg.querySelector('.bubble .sender');
            const name = senderEl ? senderEl.textContent.trim() : '';
            if (!name || name === SELF_SENDER_NAME) return;

            if (!colorMap[name]) {
                colorMap[name] = nextSlot;
                nextSlot = (nextSlot % 8) + 1;
            }
            msg.dataset.color = colorMap[name];
        });
    }

    /**
     * Scroll to the message identified by the URL fragment (#msg_<id>)
     * and apply the 'highlight' class so it stands out visually.
     */
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

    function init() {
        colorizeParticipants();
        highlightTarget();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
