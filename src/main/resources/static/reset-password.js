"use strict";

(function () {
    const form = document.getElementById("reset-form");
    const message = document.getElementById("message");
    const login = document.getElementById("login-link");
    const token = new URLSearchParams(location.search).get("token");
    history.replaceState(null, "", location.pathname);
    document.querySelectorAll("[data-password-toggle]").forEach((button) => {
        const input = button.closest(".password-wrap").querySelector("input");
        button.setAttribute("aria-label", "Show password");
        button.addEventListener("click", () => {
            const visible = input.type === "password";
            input.type = visible ? "text" : "password";
            button.setAttribute("aria-label", visible ? "Hide password" : "Show password");
            button.setAttribute("aria-pressed", String(visible));
        });
    });
    if (!token) {
        message.textContent = "This reset link is missing or invalid. Request another one.";
        form.querySelector("button[type=submit]").disabled = true;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!token) return;
        const data = new FormData(form);
        const password = String(data.get("password"));
        if (password !== String(data.get("confirm"))) {
            message.textContent = "The passwords do not match.";
            return;
        }
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;
        message.textContent = "";
        try {
            const result = await API.request("/api/auth/password/reset", {
                method: "POST", body: {token, password}
            });
            API.logout();
            message.textContent = result.message;
            message.classList.add("is-info");
            form.reset();
            login.hidden = false;
        } catch (error) {
            message.textContent = error.detail || "This reset link is invalid or has expired. Request another one.";
            message.classList.remove("is-info");
            button.disabled = false;
        }
    });
})();
