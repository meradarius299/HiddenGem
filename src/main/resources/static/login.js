"use strict";

// Login / register page. On success the JWT is stored by api.js and we go to the game.
(function () {
    const $ = (id) => document.getElementById(id);
    const form = $("auth-form");
    const message = $("message");
    const submit = $("submit");
    const emailRow = $("email-row");
    const passwordInput = form.elements.password;

    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        const input = button.closest(".password-wrap").querySelector("input");
        button.addEventListener("click", () => {
            const visible = input.type === "password";
            input.type = visible ? "text" : "password";
            button.setAttribute("aria-label", visible ? "Hide password" : "Show password");
            button.setAttribute("aria-pressed", String(visible));
        });
        button.setAttribute("aria-label", "Show password");
    });

    if (API.isLoggedIn()) {
        location.replace("/explore");
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
        $("forgot-row").hidden = on;
        $("resend-verification-link").hidden = true;
        passwordInput.autocomplete = on ? "new-password" : "current-password";
        submit.textContent = on ? "Create account" : "Log In";
        $("switch-text").textContent = on ? "Already have an account?" : "Don't have an account?";
        $("switch").textContent = on ? "Log in here" : "Create one here";
        document.title = (on ? "Register" : "Log in") + " · Hidden Gems";
        say("");
    }

    function errorText(err, google = false) {
        if (google) {
            return err.status === 401 ? (err.detail || "Google sign-in token was rejected. Check that the Client ID in login.js matches GOOGLE_CLIENT_IDS or GOOGLE_OAUTH_CLIENT_IDS, then restart the backend.")
                : err.status === 403 ? "Google sign-in was refused. Check that your Google account is verified and allowed by the OAuth consent screen."
                    : err.status === 503 ? "Google sign-in is not configured on the backend. Set GOOGLE_OAUTH_CLIENT_IDS and restart it."
                        : err.status === 400 ? "Google did not return a valid sign-in token. Try again or check the browser console."
                            : "Could not reach the server. Is the backend running?";
        }
        return err.status === 401 ? "Wrong username or password."
            : err.status === 403 ? "Verify your email before signing in."
            : err.status === 409 ? "That username or email is already taken."
                : err.status === 400 ? "Check the fields: username 3+ characters, valid email, password 6+."
            : err.status === 503 ? "Email delivery is unavailable. Check Mailpit and try again."
                        : "Could not reach the server. Is the backend running?";
    }

    function goToGame() {
        location.replace("/explore");
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
            if (registering) {
                await API.register(username, email, password);
                form.reset();
                setMode(false);
                say("Account created. Check your email for the verification link. During local development, open the Mailpit inbox at http://localhost:8025.", true);
                submit.disabled = false;
                return;
            }
            await API.login(username, password);
            goToGame();
        } catch (err) {
            say(errorText(err));
            $("resend-verification-link").hidden = err.status !== 403;
            submit.disabled = false;
        }
    });

    async function onGoogleCredential(response) {
        say("");
        try {
            await API.googleLogin(response.credential);
            goToGame();
        } catch (err) {
            say(errorText(err, true));
        }
    }

    function initGoogle() {
        fetch("/api/config/auth", {cache: "no-store"})
            .then(response => response.ok ? response.json() : Promise.reject(new Error("Auth config unavailable")))
            .then(config => loadGoogle(config.googleClientId))
            .catch(() => say("Google sign-in is not configured on the backend.", true));
    }

    function loadGoogle(clientId) {
        if (!clientId) {
            say("Google sign-in is not configured on the backend.", true);
            return;
        }
        const s = document.createElement("script");
        s.src = "https://accounts.google.com/gsi/client";
        s.async = true;
        s.onload = () => {
            if (!window.google) return;
            window.google.accounts.id.initialize({
                client_id: clientId,
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

    const params = new URLSearchParams(location.search);
    setMode(params.get("mode") === "signup");
    if (params.has("expired")) {
        say("Your session expired. Please log in again.", true);
    }
    initGoogle();
})();
