/**
 * script.js — JavaScript for timeline.html
 *
 * Features:
 *   - openThread(threadId, msgId)  Navigate to a thread page and highlight a message
 *   - filterMessages(query)        Show/hide messages matching a search query
 *   - jumpToDate(dateStr)          Scroll to the first message on a given date
 *   - colorizeTimeline()           Assign consistent sender colors on page load
 */

/** Name used for the device owner's outgoing messages — never colorized. */
const SELF_SENDER_NAME = 'Me';

/** Sender-name → color slot (1–8) cache, shared within this page load. */
const _senderColorCache = {};
let   _senderColorNext  = 1;

/**
 * Return a stable color slot (1–8) for a given sender name.
 * Outgoing messages ("Me" / empty sender) are never colorized.
 * @param {string} name
 * @returns {number|null}
 */
function getSenderColor(name) {
    if (!name || name === SELF_SENDER_NAME) return null;
    if (_senderColorCache[name]) return _senderColorCache[name];
    const slot = _senderColorNext;
    _senderColorCache[name] = slot;
    _senderColorNext = (slot % 8) + 1;
    return slot;
}

/**
 * Walk every .msg card and assign data-color from the sender name.
 * Also falls back to thread-name when the direct sender is "Me" so that
 * outgoing messages from a distinct conversation still get a thread accent.
 */
function colorizeTimeline() {
    document.querySelectorAll('#timeline .msg').forEach(card => {
        if (card.classList.contains('outgoing')) return; // keep outgoing color for self
        const senderEl     = card.querySelector('.sender');
        const threadNameEl = card.querySelector('.thread-name');
        const name = (senderEl && senderEl.textContent.trim())
                  || (threadNameEl && threadNameEl.textContent.trim())
                  || '';
        const slot = getSenderColor(name);
        if (slot) card.dataset.color = slot;
    });
}

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
    const msgs       = document.querySelectorAll('#timeline .msg');
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
            el.classList.add('jump-target');
            setTimeout(() => el.classList.remove('jump-target'), 2200);
            return;
        }
    }
    alert('No messages found on or after ' + dateStr);
}

// Run on page load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', colorizeTimeline);
} else {
    colorizeTimeline();
}
