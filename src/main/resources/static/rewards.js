"use strict";

// Rewards dialog (badges + leaderboard), opened from a header button next to "Social".
// Data: GET /api/me/badges and GET /api/leaderboard?scope=global|friends
// app.js announces what happened with two events: "key-collected" and "progress-reset".
(function () {
    const MEDAL = "\u{1F3C5}", LOCK = "\u{1F512}", PLACES = ["\u{1F947}", "\u{1F948}", "\u{1F949}"];

    const css = document.createElement("style");
    css.textContent = `
        #rewards-btn { background: transparent; color: var(--panel); border-color: var(--panel); }
        #rewards { width: min(34rem, calc(100% - 2rem)); }
        .rw-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: .8rem; }
        .rw-tabs { display: flex; gap: .4rem; flex-wrap: wrap; margin-bottom: 1rem; }
        .rw-tabs button { background: transparent; color: var(--ink); }
        .rw-tabs button[aria-pressed="true"] { background: var(--ink); color: var(--panel); }
        .rw-tabs.small { margin-bottom: .6rem; }
        .rw-tabs.small button { padding: .25rem .7rem; font-size: .9rem; }
        .rw-lead { margin: 0 0 .8rem; color: var(--muted); }
        .rw-badges, .rw-board { list-style: none; margin: 0; padding: 0; }
        .rw-badges { display: grid; grid-template-columns: repeat(auto-fit, minmax(9.5rem, 1fr)); gap: .7rem; }
        .rw-badge { display: grid; justify-items: center; text-align: center; gap: .2rem; padding: .9rem .6rem;
            border: 1px solid var(--line); border-radius: 12px; opacity: .8; }
        .rw-badge.earned { opacity: 1; border-color: var(--brass); background: #fbf3e0; }
        .rw-medal { font-size: 2.2rem; line-height: 1.2; filter: grayscale(1); }
        .rw-badge.earned .rw-medal { filter: none; }
        .rw-sub { font-size: .85rem; color: var(--muted); }
        .rw-bar { display: block; width: 100%; height: 6px; background: var(--line); border-radius: 3px;
            overflow: hidden; margin-top: .3rem; }
        .rw-bar > span { display: block; height: 100%; background: var(--moss); }
        .rw-board { display: grid; gap: .3rem; }
        .rw-row { display: grid; grid-template-columns: 2.4rem 1fr auto; gap: .5rem; align-items: center;
            padding: .5rem .6rem; border-radius: 8px; }
        .rw-row.me { background: #e6efeb; font-weight: 700; }
        .rw-rank { color: var(--muted); }
        .rw-mine { margin-top: .5rem; border-top: 1px dashed var(--line); padding-top: .5rem; }
        .rw-muted { color: var(--muted); margin: .4rem 0; }
        .rw-toast { position: fixed; inset: auto auto 1.2rem 50%; transform: translateX(-50%); margin: 0; border: 0;
            background: var(--ink); color: #e6c27a; padding: .7rem 1.1rem; border-radius: 999px;
            box-shadow: 0 4px 14px rgba(0, 0, 0, .3); }
    `;
    document.head.append(css);

    function h(tag, props, ...kids) {
        const e = document.createElement(tag);
        for (const [k, v] of Object.entries(props || {})) {
            if (k === "class") e.className = v;
            else if (k === "text") e.textContent = v;
            else if (k.startsWith("on")) e.addEventListener(k.slice(2), v);
            else e.setAttribute(k, v);
        }
        kids.flat().forEach((c) => { if (c != null) e.append(c); });
        return e;
    }

    const note = (text) => h("p", {class: "rw-muted", text});

    let dlg, body;
    const tabButtons = {};
    let view = "badges", scope = "global";
    let badgeData = null, boardData = null;

    async function fetchBadges() {
        try { badgeData = await API.request("/api/me/badges"); } catch (err) { badgeData = null; }
    }

    async function fetchBoard() {
        try { boardData = await API.request("/api/leaderboard?scope=" + scope); } catch (err) { boardData = null; }
    }

    // ---------- badges ----------
    function badgeTile(b, keys) {
        const have = Math.min(keys, b.keysRequired);
        const detail = b.earned
            ? h("span", {class: "rw-sub", text: "Earned " + new Date(b.earnedAt).toLocaleDateString()})
            : [h("span", {class: "rw-bar"}, h("span", {style: `width:${Math.round((have / b.keysRequired) * 100)}%`})),
                h("span", {class: "rw-sub", text: `${have} / ${b.keysRequired} keys`})];
        return h("li", {class: "rw-badge" + (b.earned ? " earned" : "")},
            h("span", {class: "rw-medal", text: b.earned ? MEDAL : LOCK}),
            h("strong", {text: b.title}),
            h("span", {class: "rw-sub", text: b.description}),
            detail);
    }

    function renderBadges() {
        if (!badgeData) return [note("Could not load your badges.")];
        const earned = badgeData.badges.filter((b) => b.earned).length;
        return [
            h("p", {class: "rw-lead", text: `${earned} of ${badgeData.badges.length} earned · ${badgeData.keys} keys collected`}),
            h("ul", {class: "rw-badges"}, badgeData.badges.map((b) => badgeTile(b, badgeData.keys)))
        ];
    }

    // ---------- leaderboard ----------
    function scopeTabs() {
        const tab = (key, label) => h("button", {
            type: "button", "aria-pressed": String(scope === key),
            onclick: async () => { scope = key; await fetchBoard(); render(); }
        }, label);
        return h("div", {class: "rw-tabs small"}, tab("global", "Everyone"), tab("friends", "Friends"));
    }

    function boardRow(e, me) {
        const mine = me && e.userId === me.userId;
        return h("li", {class: "rw-row" + (mine ? " me" : "")},
            h("span", {class: "rw-rank", text: e.rank && e.rank <= 3 ? PLACES[e.rank - 1] : "#" + e.rank}),
            h("span", {class: "rw-name", text: mine ? e.username + " (you)" : e.username}),
            h("span", {class: "rw-keys", text: e.keys + (e.keys === 1 ? " key" : " keys")}));
    }

    function renderBoard() {
        if (!boardData) return [note("Could not load the leaderboard.")];
        const me = boardData.me, top = boardData.top;
        const lead = me && me.rank
            ? (scope === "friends" ? `You are #${me.rank} among you and your friends` : `You are #${me.rank} on the leaderboard`)
            : "Collect a key to join the leaderboard.";
        const nodes = [scopeTabs(), h("p", {class: "rw-lead", text: lead})];
        if (!top.length) {
            nodes.push(note(scope === "friends" ? "None of you has collected a key yet." : "Nobody has collected a key yet. Be the first!"));
        } else {
            nodes.push(h("ol", {class: "rw-board"}, top.map((e) => boardRow(e, me))));
        }
        if (me && me.rank && me.rank > top.length) {
            nodes.push(h("ol", {class: "rw-board rw-mine"}, boardRow(me, me))); // outside the top 10
        }
        return nodes;
    }

    // ---------- dialog ----------
    function render() {
        Object.entries(tabButtons).forEach(([key, b]) => b.setAttribute("aria-pressed", String(key === view)));
        if (!dlg.open) return;
        body.replaceChildren(...(view === "badges" ? renderBadges() : renderBoard()));
    }

    async function refresh() {
        await Promise.all([fetchBadges(), fetchBoard()]);
        render();
    }

    // A popover sits in the browser's top layer, so it stays visible above an open dialog.
    function toast(text) {
        const t = h("div", {class: "rw-toast", role: "status", popover: "manual", text});
        document.body.append(t);
        try { t.showPopover(); } catch (err) { /* older browser: the CSS still shows it as a fixed box */ }
        setTimeout(() => t.remove(), 5000);
    }

    function init() {
        body = h("div", {class: "rw-body"});
        const tabBtn = (key, label) => {
            const b = h("button", {type: "button", onclick: () => { view = key; render(); }}, label);
            tabButtons[key] = b;
            return b;
        };
        dlg = h("dialog", {id: "rewards"},
            h("div", {class: "rw-head"},
                h("h2", {text: "Rewards"}),
                h("button", {type: "button", class: "plain", onclick: () => dlg.close()}, "Close")),
            h("div", {class: "rw-tabs"}, tabBtn("badges", "Badges"), tabBtn("board", "Leaderboard")),
            body);
        document.body.append(dlg);

        const account = document.getElementById("account");
        const btn = h("button", {
            type: "button", id: "rewards-btn",
            onclick: () => { dlg.showModal(); render(); refresh(); }
        }, "Rewards");
        const social = document.getElementById("social-btn");
        if (social) social.after(btn); else account.prepend(btn);

        window.addEventListener("key-collected", (e) => {
            ((e.detail && e.detail.newBadges) || []).forEach((name) => toast(MEDAL + " Badge earned: " + name));
            refresh();
        });
        window.addEventListener("progress-reset", refresh);
        setInterval(() => {
            if (!document.hidden && dlg.open && view === "board") fetchBoard().then(render);
        }, 30000);

        refresh();
    }

    // boot.js sets GEMS_DATA and builds the header once the player is logged in and the gems are loaded.
    const waiting = setInterval(() => {
        if (document.getElementById("account") && window.GEMS_DATA && API.isLoggedIn()) {
            clearInterval(waiting);
            if (!window.IS_DEMO) init(); // demo gems are not saved on the server, so there is nothing to show
        }
    }, 300);
})();