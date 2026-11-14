"use strict";

(function () {
    const form = document.getElementById("action-form");
    const message = document.getElementById("message");
    const endpoint = document.currentScript.dataset.endpoint;
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;
        message.textContent = "";
        try {
            const result = await API.request(endpoint, {
                method: "POST",
                body: {email: String(new FormData(form).get("email")).trim()}
            });
            message.textContent = result.message;
            message.classList.add("is-info");
            form.reset();
        } catch (error) {
            message.textContent = error.detail || "Could not contact the server. Try again shortly.";
            message.classList.remove("is-info");
        } finally {
            button.disabled = false;
        }
    });
})();
