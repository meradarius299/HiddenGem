"use strict";

(async function () {
    const message = document.getElementById("message");
    const login = document.getElementById("login-link");
    const token = new URLSearchParams(location.search).get("token");
    history.replaceState(null, "", location.pathname);
    if (!token) {
        message.textContent = "This verification link is missing or invalid.";
        login.hidden = false;
        return;
    }
    try {
        const result = await API.request("/api/auth/verify-email", {method: "POST", body: {token}});
        message.textContent = result.message;
    } catch (error) {
        message.textContent = error.detail || "This verification link is invalid or has expired. Request a new one and try again.";
    }
    login.hidden = false;
})();
