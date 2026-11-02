"use strict";

// Runs before app.js: makes sure the user is logged in, loads the gems from the
// backend, then starts the game (app.js reads window.GEMS_DATA).
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
        verified: g.verified,
    });

    // Sample gems from gems.js, used when the database has no gems yet.
    async function demoGems() {
        const text = await (await fetch("gems.js")).text();
        return new Function(text + "\nreturn GEMS;")();
    }

    async function start() {
        if (!API.isLoggedIn()) {
            location.replace("login.html");
            return;
        }
        let gems;
        try {
            gems = (await API.getGems()).map(toGame);
        } catch (err) {
            if (err.status === 401 || err.status === 403) {
                API.logout();
                location.replace("login.html?expired=1");
                return;
            }
            gems = [];
        }
        const demo = gems.length === 0;
        if (demo) gems = await demoGems();

        window.GEMS_DATA = gems;
        window.CURRENT_USER = API.username();

        const account = document.createElement("div");
        account.id = "account";
        const who = document.createElement("span");
        who.textContent = API.username() + (demo ? " (demo gems)" : "");
        const out = document.createElement("button");
        out.type = "button";
        out.className = "plain";
        out.textContent = "Log out";
        account.append(who, out);
        document.querySelector("header").append(account);

        const s = document.createElement("script");
        s.src = "app.js";
        document.body.append(s);
    }

    start();
})();