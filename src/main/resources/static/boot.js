"use strict";

// Runs before app.js: makes sure the user is logged in, loads the gems from the
// backend, then starts the game (app.js reads window.GEMS_DATA).
(function () {
    const css = document.createElement("style");
    css.textContent = `
        #auth { border: 1px solid var(--line, #ccc); border-radius: 12px; padding: 1.5rem; width: min(92vw, 360px); }
        #auth::backdrop { background: rgba(0, 0, 0, .55); }
        #auth form { display: grid; gap: .8rem; margin: 1rem 0 .5rem; }
        #auth label { display: grid; gap: .25rem; font-size: .9rem; }
        #auth input { padding: .55rem; font: inherit; }
        #auth button { padding: .6rem; font: inherit; cursor: pointer; }
        .auth-msg { color: #b3261e; min-height: 1.2em; margin: 0; }
        #account { display: flex; gap: .6rem; align-items: center; font-size: .9rem; }
    `;
    document.head.append(css);

    function askLogin(message) {
        return new Promise((resolve) => {
            let registering = false;
            const dlg = document.createElement("dialog");
            dlg.id = "auth";
            dlg.innerHTML = `
                <h2>Hidden Gems</h2>
                <p class="auth-msg"></p>
                <form novalidate>
                    <label>Username <input name="username" autocomplete="username" required></label>
                    <label class="reg" hidden>Email <input name="email" type="email" autocomplete="email"></label>
                    <label>Password <input name="password" type="password" autocomplete="current-password" required></label>
                    <button type="submit">Log in</button>
                </form>
                <button type="button" class="plain switch">No account? Register</button>`;
            const form = dlg.querySelector("form");
            const msg = dlg.querySelector(".auth-msg");
            const go = form.querySelector("button");
            const sw = dlg.querySelector(".switch");
            const reg = dlg.querySelector(".reg");
            msg.textContent = message || "";

            sw.addEventListener("click", () => {
                registering = !registering;
                reg.hidden = !registering;
                go.textContent = registering ? "Create account" : "Log in";
                sw.textContent = registering ? "Have an account? Log in" : "No account? Register";
                msg.textContent = "";
            });

            form.addEventListener("submit", async (e) => {
                e.preventDefault();
                const f = new FormData(form);
                const username = String(f.get("username")).trim();
                const password = String(f.get("password"));
                msg.textContent = "";
                go.disabled = true;
                try {
                    if (registering) await API.register(username, String(f.get("email")).trim(), password);
                    else await API.login(username, password);
                    dlg.close();
                    dlg.remove();
                    resolve();
                } catch (err) {
                    msg.textContent =
                        err.status === 401 ? "Wrong username or password."
                            : err.status === 409 ? "That username or email is already taken."
                                : err.status === 400 ? "Check the fields: username 3+ characters, valid email, password 6+."
                                    : "Could not reach the server. Is the backend running?";
                } finally {
                    go.disabled = false;
                }
            });

            dlg.addEventListener("cancel", (e) => e.preventDefault()); // Esc must not skip the login
            document.body.append(dlg);
            dlg.showModal();
        });
    }

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
        owner: g.owner
    });

    // Sample gems from gems.js, used when the database has no gems yet.
    async function demoGems() {
        const text = await (await fetch("gems.js")).text();
        return new Function(text + "\nreturn GEMS;")();
    }

    async function start(message) {
        if (!API.isLoggedIn()) await askLogin(message);
        let gems;
        try {
            gems = (await API.getGems()).map(toGame);
        } catch (err) {
            if (err.status === 401 || err.status === 403) {
                API.logout();
                return start("Your session expired. Please log in again.");
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
        out.addEventListener("click", () => { API.logout(); location.reload(); });
        account.append(who, out);
        document.querySelector("header").append(account);

        const s = document.createElement("script");
        s.src = "app.js";
        document.body.append(s);
    }

    start();
})();