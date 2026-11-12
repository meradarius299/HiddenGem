"use strict";

(function () {
    const ICONS = {
        NEW_GEM: "\u{1F4CD}",
        FRIEND_REQUEST: "\u{1F91D}",
        BUMP: "\u{1F44B}",
        BADGE_EARNED: "\u{1F3C5}",
        TASK_COMPLETED: "\u{1F3AF}",
        USER_REQUEST: "\u23F3",
        REVIEW_ADDED: "\u2705",
        INACTIVITY_REMINDER: "\u23F0"
    };

    const css = document.createElement("style");
    css.textContent = `
        #account .plain { color: var(--panel); border-color: var(--panel); }
        #social-btn { background: transparent; color: var(--panel); border-color: var(--panel); }
        #social-badge { background: #b3261e; color: #fff; border-radius: 999px; font-size: .75rem;
            padding: 0 .45rem; margin-left: .45rem; }
        #social-badge[hidden] { display: none; }
        #social { width: min(40rem, calc(100% - 2rem)); }
        .soc-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: .8rem; }
        .soc-tabs { display: flex; gap: .4rem; flex-wrap: wrap; margin-bottom: 1rem; }
        .soc-tabs button { background: transparent; color: var(--ink); }
        .soc-tabs button[aria-pressed="true"] { background: var(--ink); color: var(--panel); }
        #social h3 { margin: 1.1rem 0 .3rem; font-size: 1.05rem; }
        .soc-list { list-style: none; margin: .5rem 0; padding: 0; }
        .soc-list li { border-bottom: 1px solid var(--line); }
        .soc-list li.unread { background: #fff6e0; }
        .soc-item { width: 100%; text-align: left; background: none; border: 0; color: inherit;
            padding: .7rem .4rem; display: flex; gap: .7rem; align-items: flex-start; }
        .soc-item:hover { background: #e6efeb; }
        .soc-icon { font-size: 1.3rem; line-height: 1.2; }
        .soc-text, .soc-feed > div { display: grid; gap: .1rem; }
        .soc-msg { font-family: "Alegreya", Georgia, serif; }
        .soc-time { font-size: .8rem; color: var(--muted); }
        .soc-row, .soc-feed { display: flex; justify-content: space-between; align-items: center;
            gap: .8rem; padding: .6rem .4rem; }
        .soc-actions { display: flex; gap: .4rem; }
        .soc-search { display: flex; gap: .5rem; }
        .soc-search input { flex: 1; font: inherit; padding: .5rem; }
        .soc-muted { color: var(--muted); margin: .5rem 0; }
        .soc-status { min-height: 1.4rem; margin: .8rem 0 0; }
        .soc-error { color: #9b2c2c; }
    `;
    document.head.append(css);

    const $ = (id) => document.getElementById(id);

    function h(tag, props, ...kids) {
        const e = document.createElement(tag);
        for (const [k, v] of Object.entries(props || {})) {
            if (k === "class") e.className = v;
            else if (k === "text") e.textContent = v;
            else if (k.startsWith("on")) e.addEventListener(k.slice(2), v);
            else e.setAttribute(k, v);
        }
        kids.flat().forEach((c) => {
            if (c != null) e.append(c);
        });
        return e;
    }

    function ago(iso) {
        const t = new Date(iso).getTime();
        if (Number.isNaN(t)) return "";
        const s = Math.max(0, Math.floor((Date.now() - t) / 1000));
        if (s < 60) return "just now";
        if (s < 3600) return Math.floor(s / 60) + " min ago";
        if (s < 86400) return Math.floor(s / 3600) + " h ago";
        return Math.floor(s / 86400) + " d ago";
    }

    function errText(err, overrides) {
        if (overrides && overrides[err.status]) return overrides[err.status];
        switch (err.status) {
            case 400:
                return "That request is not valid.";
            case 401:
            case 403:
                return "Your session expired. Log out and log in again.";
            case 404:
                return "Not found.";
            case 409:
                return "Already done, or still pending.";
            default:
                return "Could not reach the server. Is the backend running?";
        }
    }

    let dlg, body, status, badge;
    const tabButtons = {};
    let tab = "notifications";
    let lastCount = -1;

    function say(text, isError) {
        status.textContent = text || "";
        status.className = "soc-status" + (isError ? " soc-error" : "");
    }

    async function refreshBadge(rerender) {
        try {
            const r = await API.request("/api/notifications/unread-count");
            const count = r && r.count ? r.count : 0;
            badge.textContent = String(count);
            badge.hidden = count === 0;
            const changed = lastCount !== -1 && count !== lastCount;
            lastCount = count;
            if (rerender && changed && dlg.open && tab === "notifications") show("notifications");
        } catch (err) {
        }
    }

    function goToGem(id) {
        const g = (window.GEMS_DATA || []).find((x) => x.id === id);
        if (g) {
            try {
                map.flyTo([g.lat, g.lon], 17);
                dlg.close();
                return;
            } catch (err) {
            }
        }
        location.reload();
    }

    async function openNotification(n) {
        if (!n.read) {
            try {
                await API.request("/api/notifications/" + n.id + "/read", {method: "PATCH"});
            } catch (err) {
            }
            await refreshBadge(false);
        }
        if (n.type === "NEW_GEM" && n.gemId) goToGem(n.gemId);
        else if (n.type === "FRIEND_REQUEST") show("friends");
        else show("notifications");
    }

    async function buildNotifications() {
        const list = await API.request("/api/notifications");
        if (!list.length) {
            return [h("p", {
                class: "soc-muted",
                text: "No notifications yet. Add a friend, then have them add a gem."
            })];
        }
        const markAll = h("button", {
            type: "button", class: "plain",
            onclick: async () => {
                try {
                    await API.request("/api/notifications/read-all", {method: "PATCH"});
                } catch (err) {
                    say(errText(err), true);
                    return;
                }
                await refreshBadge(false);
                show("notifications");
            }
        }, "Mark all as read");

        const ul = h("ul", {class: "soc-list"}, list.map((n) =>
            h("li", {class: n.read ? "" : "unread"},
                h("button", {type: "button", class: "soc-item", onclick: () => openNotification(n)},
                    h("span", {class: "soc-icon", text: ICONS[n.type] || "\u{1F514}"}),
                    h("span", {class: "soc-text"},
                        h("span", {class: "soc-msg", text: n.message}),
                        h("span", {class: "soc-time", text: ago(n.createdAt)}))))));
        return [markAll, ul];
    }

    async function buildFeed() {
        const gems = await API.request("/api/feed");
        if (!gems.length) {
            return [h("p", {
                class: "soc-muted",
                text: "Nothing here yet. When a friend adds a gem, it shows up here."
            })];
        }
        return [h("ul", {class: "soc-list"}, gems.map((g) =>
            h("li", {},
                h("div", {class: "soc-feed"},
                    h("div", {},
                        h("strong", {text: g.name}),
                        h("span", {
                            class: "soc-time",
                            text: g.owner + " · " + (g.category || "gem") + " · " + ago(g.createdAt)
                        })),
                    h("button", {type: "button", class: "plain", onclick: () => goToGem(g.id)}, "Show on map")))))];
    }

    async function answer(requestId, action) {
        try {
            await API.request("/api/friends/requests/" + requestId + "/" + action, {method: "POST"});
        } catch (err) {
            say(errText(err, {404: "That request no longer exists.", 409: "That request was already answered."}), true);
            return;
        }
        await refreshBadge(false);
        show("friends");
    }

    async function removeFriend(user) {
        if (!confirm("Remove " + user.username + " from your friends?")) return;
        try {
            await API.request("/api/friends/" + user.id, {method: "DELETE"});
        } catch (err) {
            say(errText(err, {404: "You are not friends."}), true);
            return;
        }
        show("friends");
    }

    async function buildFriends() {
        const [friends, incoming] = await Promise.all([
            API.request("/api/friends"),
            API.request("/api/friends/requests/incoming")
        ]);

        const userRow = (u) => {
            const add = h("button", {
                type: "button",
                onclick: async () => {
                    add.disabled = true;
                    try {
                        await API.request("/api/friends/requests", {method: "POST", body: {username: u.username}});
                        add.textContent = "Request sent";
                        say("Request sent to " + u.username + ".");
                    } catch (err) {
                        add.disabled = false;
                        say(errText(err, {
                            400: "You can't add yourself.",
                            404: "That user no longer exists.",
                            409: "You are already friends, or a request is pending."
                        }), true);
                    }
                }
            }, "Add friend");
            return h("li", {}, h("div", {class: "soc-row"}, h("span", {text: u.username}), add));
        };

        const input = h("input", {
            type: "search",
            placeholder: "Search by username",
            autocomplete: "off",
            "aria-label": "Search users"
        });
        const results = h("ul", {class: "soc-list"});
        const form = h("form", {
            class: "soc-search",
            onsubmit: async (e) => {
                e.preventDefault();
                const q = input.value.trim();
                if (q.length < 2) {
                    say("Type at least 2 letters.", true);
                    return;
                }
                say("");
                try {
                    const users = await API.request("/api/users/search?q=" + encodeURIComponent(q));
                    results.replaceChildren(...(users.length
                        ? users.map(userRow)
                        : [h("li", {}, h("p", {class: "soc-muted", text: "No users found."}))]));
                } catch (err) {
                    say(errText(err), true);
                }
            }
        }, input, h("button", {type: "submit"}, "Search"));

        const nodes = [h("h3", {text: "Add a friend"}), form, results];

        if (incoming.length) {
            nodes.push(
                h("h3", {text: "Friend requests (" + incoming.length + ")"}),
                h("ul", {class: "soc-list"}, incoming.map((r) =>
                    h("li", {},
                        h("div", {class: "soc-row"},
                            h("span", {text: r.requesterUsername + " wants to be your friend"}),
                            h("span", {class: "soc-actions"},
                                h("button", {type: "button", onclick: () => answer(r.id, "accept")}, "Accept"),
                                h("button", {
                                    type: "button",
                                    class: "plain",
                                    onclick: () => answer(r.id, "reject")
                                }, "Reject")))))));
        }

        nodes.push(h("h3", {text: "Your friends (" + friends.length + ")"}));
        if (friends.length) {
            nodes.push(h("ul", {class: "soc-list"}, friends.map((u) =>
                h("li", {},
                    h("div", {class: "soc-row"},
                        h("span", {text: u.username}),
                        h("button", {type: "button", class: "plain", onclick: () => removeFriend(u)}, "Remove"))))));
        } else {
            nodes.push(h("p", {class: "soc-muted", text: "No friends yet. Search for a username above."}));
        }
        return nodes;
    }

    async function show(name) {
        tab = name;
        Object.entries(tabButtons).forEach(([key, b]) => b.setAttribute("aria-pressed", String(key === name)));
        say("");
        body.replaceChildren(h("p", {class: "soc-muted", text: "Loading..."}));
        try {
            const nodes = await (name === "notifications" ? buildNotifications()
                : name === "feed" ? buildFeed()
                    : buildFriends());
            if (tab === name) body.replaceChildren(...nodes);
        } catch (err) {
            if (tab === name) body.replaceChildren(h("p", {class: "soc-error", text: errText(err)}));
        }
    }

    function init() {
        badge = h("span", {id: "social-badge", hidden: ""});
        body = h("div", {class: "soc-body"});
        status = h("p", {class: "soc-status", role: "status"});

        const tabBtn = (key, label) => {
            const b = h("button", {type: "button", onclick: () => show(key)}, label);
            tabButtons[key] = b;
            return b;
        };

        dlg = h("dialog", {id: "social"},
            h("div", {class: "soc-head"},
                h("h2", {text: "Social"}),
                h("button", {type: "button", class: "plain", onclick: () => dlg.close()}, "Close")),
            h("div", {class: "soc-tabs"},
                tabBtn("notifications", "Notifications"),
                tabBtn("feed", "Friends' gems"),
                tabBtn("friends", "Friends")),
            body,
            status);
        document.body.append(dlg);

        const btn = h("button", {
            type: "button", id: "social-btn",
            onclick: () => {
                dlg.showModal();
                show(tab);
            }
        }, "Social", badge);
        $("account").prepend(btn);

        refreshBadge(false);
        setInterval(() => {
            if (!document.hidden) refreshBadge(true);
        }, 10000);
    }

    const waiting = setInterval(() => {
        if ($("account") && API.isLoggedIn()) {
            clearInterval(waiting);
            init();
        }
    }, 300);
})();