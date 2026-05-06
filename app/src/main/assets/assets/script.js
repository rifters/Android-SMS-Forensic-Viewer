/**
 * script.js — JavaScript for timeline.html
 *
 * Features:
 *   - openThread(threadId, msgId)  Navigate to a thread page and highlight a message
 *   - filterMessages(query)        Show/hide messages matching a search query
 *   - jumpToDate(dateStr)          Scroll to the first message on a given date
 */

/**
 * Navigate to the thread page for the given threadId, scrolling to msgId.
 * @param {number} threadId
 * @param {number} msgId
 */
function openThread(threadId, msgId) {
    window.location.href = `thread_${threadId}.html#msg_${msgId}`;
}

/**
 * Filter message cards in the timeline based on a search query.
 * Matches against sender name, thread name, and message body.
 * @param {string} query
 */
function filterMessages(query) {
    const q = query.trim().toLowerCase();
    const msgs = document.querySelectorAll('#timeline .msg');
    const separators = document.querySelectorAll('#timeline .date-separator');

    if (!q) {
        msgs.forEach(el => el.classList.remove('hidden'));
        separators.forEach(el => el.style.display = '');
        return;
    }

    msgs.forEach(el => {
        const text = el.textContent.toLowerCase();
        el.classList.toggle('hidden', !text.includes(q));
    });

    // Hide date separators with no visible messages after them
    separators.forEach(sep => {
        let next = sep.nextElementSibling;
        let hasVisible = false;
        while (next && !next.classList.contains('date-separator')) {
            if (next.classList.contains('msg') && !next.classList.contains('hidden')) {
                hasVisible = true;
                break;
            }
            next = next.nextElementSibling;
        }
        sep.style.display = hasVisible ? '' : 'none';
    });
}

/**
 * Scroll to the first message on or after a given date string (yyyy-MM-dd).
 * @param {string} dateStr  e.g. "2024-05-05"
 */
function jumpToDate(dateStr) {
    if (!dateStr) return;
    const target = new Date(dateStr).getTime();
    const msgs = document.querySelectorAll('#timeline .msg');
    for (const el of msgs) {
        const ts = parseInt(el.dataset.ts, 10);
        if (!isNaN(ts) && ts >= target) {
            el.scrollIntoView({ behavior: 'smooth', block: 'start' });
            el.style.outline = '2px solid #E65100';
            setTimeout(() => { el.style.outline = ''; }, 2000);
            return;
        }
    }
    alert('No messages found on or after ' + dateStr);
}
