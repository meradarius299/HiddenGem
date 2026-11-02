"use strict";

// Login / register page. On success the JWT is stored by api.js and we go to the game.
(function () {
    // Web OAuth client ID for "Continue with Google". Leave empty to hide the button.
    // The same ID must also be listed in the backend property app.google.client-ids.
    const GOOGLE_CLIENT_ID = "820459243630-b8d6pc0gkdrvl9rgfma149q7n56evs55.apps.googleusercontent.com";

    const $ = (id) => document.getElementById(id);
    const form = $("auth-form");
    const message = $("message");
    const submit = $("submit");
    const emailRow = $("email-row");
    const passwordInput = form.elements.password;

    if (API.isLoggedIn()) {
        location.replace("index.html");
        return;
    }

    let registering = false;

    function say(text, info) {
        message.textContent = text;
        message.classList.toggle("is-info", !!info);
    }

    function setMode(on) {
        registering = on;
        emailRow.hidden = !on;
        passwordInput.autocomplete = on ? "new-password" : "current-password";
        submit.textContent = on ? "Create account" : "Log In";
        $("switch-text").textContent = on ? "Already have an account?" : "Don't have an account?";
        $("switch").textContent = on ? "Log in here" : "Create one here";
        document.title = (on ? "Register" : "Log in") + " · Hidden Gems";
        say("");
    }

    function errorText(err) {
        return err.status === 401 ? "Wrong username or password."
            : err.status === 409 ? "That username or email is already taken."
                : err.status === 400 ? "Check the fields: username 3+ characters, valid email, password 6+."
                    : err.status === 503 ? "This sign-in method is not available right now."
                        : "Could not reach the server. Is the backend running?";
    }

    function goToGame() {
        location.replace("index.html");
    }

    $("switch").addEventListener("click", (e) => {
        e.preventDefault();
        setMode(!registering);
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const data = new FormData(form);
        const username = String(data.get("username")).trim();
        const email = String(data.get("email")).trim();
        const password = String(data.get("password"));

        if (!username || !password) { say("Enter your username and password."); return; }
        if (registering && !email) { say("Enter your email address."); return; }

        say("");
        submit.disabled = true;
        try {
            if (registering) await API.register(username, email, password);
            else await API.login(username, password);
            goToGame();
        } catch (err) {
            say(errorText(err));
            submit.disabled = false;
        }
    });

    async function onGoogleCredential(response) {
        say("");
        try {
            await API.googleLogin(response.credential);
            goToGame();
        } catch (err) {
            say(errorText(err));
        }
    }

    function initGoogle() {
        if (!GOOGLE_CLIENT_ID) return;
        const s = document.createElement("script");
        s.src = "https://accounts.google.com/gsi/client";
        s.async = true;
        s.onload = () => {
            if (!window.google) return;
            window.google.accounts.id.initialize({
                client_id: GOOGLE_CLIENT_ID,
                callback: onGoogleCredential
            });
            window.google.accounts.id.renderButton($("google-slot"), {
                theme: "outline",
                size: "large",
                width: 320,
                text: "continue_with"
            });
            $("google-block").hidden = false;
        };
        document.head.append(s);
    }

    if (new URLSearchParams(location.search).has("expired")) {
        say("Your session expired. Please log in again.", true);
    }
    initGoogle();
})();
