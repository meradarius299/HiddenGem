"use strict";
(function () {
    const css = document.createElement("style");
    css.textContent = `#account { display: flex; gap: .6rem; align-items: center; font-size: .9rem; }`;
    document.head.append(css);

    // Backend gem -> the shape app.js expects. The backend has no hint or quiz yet,
    // so the category becomes the hint and the description becomes the story.
    const toGame = (g) => ({
        id: g.id,
        name: g.name,
        hint: g.category ? `A ${g.category.toLowerCase()} worth discovering` : "A hidden spot nearby",
        lat: g.latitude,
        lon: g.longitude,
        story: g.description || "Nobody has written the story of this place yet.",
        quiz: null,
        visible: g.visible,
        owner: g.owner
    });

    // Sample gems from gems.js, used when the database has no gems yet.
    async function demoGems() {
        const text = await (await fetch("gems.js")).text();
        return new Function(text + "\nreturn GEMS;")();
    }

    async function start() {
        if (!API.isLoggedIn()) { location.replace("/login"); return; }
        let gems;
        try {
            gems = (await API.getGems()).map(toGame);
        } catch (err) {
            if (err.status === 401 || err.status === 403) {
                API.logout();
                location.replace("/login?expired=1");
                return;
            }
            gems = [];
        }
        const demo = gems.length === 0;
        if (demo) gems = await demoGems();

        let discoveries = [];
        if (!demo) {
            try { discoveries = await API.request("/api/feed/discoveries"); } catch { /* map works without friend discoveries */ }
        }

        window.GEMS_DATA = gems;
        window.FRIEND_DISCOVERIES = discoveries.reduce((byGem, discovery) => {
            (byGem[discovery.gemId] ||= []).push(discovery.discoverer);
            return byGem;
        }, {});
        window.CURRENT_USER = API.username();
        window.IS_ADMIN = API.isAdmin();
        window.IS_DEMO = demo;
        window.COLLECTED_IDS = null; // null = could not load; app.js falls back to the copy in this browser
        if (!demo) {
            try { window.COLLECTED_IDS = await API.getCollected(); } catch { /* keep the browser copy */ }
        }

        const account = document.createElement("div");
        account.id = "account";
        const who = document.createElement("span");
        who.textContent = API.username() + (API.isAdmin() ? " · Admin" : "") + (demo ? " (demo gems)" : "");
        const out = document.createElement("button");
        out.type = "button";
        out.className = "plain";
        out.textContent = "Log out";
        out.addEventListener("click", () => { API.logout(); location.assign("/"); });
        account.append(who, out);
        if (API.isAdmin()) {
            const hiddenButton = document.createElement("button");
            hiddenButton.type = "button";
            hiddenButton.textContent = "Hidden gems";
            const hiddenDialog = document.createElement("dialog");
            hiddenDialog.id = "hidden-gems-dialog";
            hiddenButton.addEventListener("click", async () => {
                hiddenDialog.replaceChildren();
                const title = document.createElement("h2");
                title.textContent = "Hidden gems";
                hiddenDialog.append(title);
                try {
                    const hiddenGems = await API.getHiddenGems();
                    if (!hiddenGems.length) {
                        const empty = document.createElement("p");
                        empty.textContent = "No hidden gems.";
                        hiddenDialog.append(empty);
                    }
                    hiddenGems.forEach(gem => {
                        const row = document.createElement("p");
                        const name = document.createElement("span");
                        name.textContent = `${gem.name} · ${gem.owner}`;
                        const restore = document.createElement("button");
                        restore.type = "button";
                        restore.textContent = "Restore to map";
                        restore.addEventListener("click", async () => {
                            restore.disabled = true;
                            try {
                                await API.setGemVisibility(gem.id, true);
                                row.remove();
                            } catch {
                                restore.disabled = false;
                            }
                        });
                        row.append(name, document.createTextNode(" "), restore);
                        hiddenDialog.append(row);
                    });
                } catch {
                    const error = document.createElement("p");
                    error.textContent = "Could not load hidden gems.";
                    hiddenDialog.append(error);
                }
                const close = document.createElement("button");
                close.type = "button";
                close.textContent = "Close";
                close.addEventListener("click", () => hiddenDialog.close());
                hiddenDialog.append(close);
                hiddenDialog.showModal();
            });
            hiddenButton.id = "hidden-gems-button";
            document.querySelector("aside").insertBefore(hiddenButton, document.querySelector("#list"));
            document.body.append(hiddenDialog);
        }
        document.querySelector("header").append(account);

        window.GEM_MAP = await HiddenGemGlobe.create(gems);

        const s = document.createElement("script");
        s.src = "app.js";
        document.body.append(s);
    }

    start();
})();
