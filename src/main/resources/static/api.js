"use strict";

const API = (() => {
    const TOKEN = "hg.token", USER = "hg.user";
    const store = {
        get: (k) => {
            try {
                return localStorage.getItem(k);
            } catch {
                return null;
            }
        },
        set: (k, v) => {
            try {
                localStorage.setItem(k, v);
            } catch {
            }
        },
        del: (k) => {
            try {
                localStorage.removeItem(k);
            } catch {
            }
        }
    };

    async function request(path, {method = "GET", body} = {}) {
        const headers = {};
        if (body) headers["Content-Type"] = "application/json";
        const token = store.get(TOKEN);
        if (token) headers.Authorization = `Bearer ${token}`;
        const res = await fetch(path, {method, headers, body: body ? JSON.stringify(body) : undefined});
        if (!res.ok) {
            const err = new Error(`HTTP ${res.status}`);
            err.status = res.status;
            throw err;
        }
        return res.status === 204 ? null : res.json();
    }

    const save = (r) => {
        store.set(TOKEN, r.token);
        store.set(USER, r.username);
        return r;
    };

    return {
        request,
        isLoggedIn: () => !!store.get(TOKEN),
        username: () => store.get(USER) || "",
        logout: () => {
            store.del(TOKEN);
            store.del(USER);
        },
        login: async (username, password) =>
            save(await request("/api/auth/login", {method: "POST", body: {username, password}})),
        register: async (username, email, password) => {
            const r = await request("/api/auth/register", {method: "POST", body: {username, email, password}});
            return r && r.token ? save(r) : API.login(username, password);
        },
        googleLogin: async (idToken) =>
            save(await request("/api/auth/google", {method: "POST", body: {idToken}})),
        getGems: () => request("/api/gems"),
        createGem: (gem) => request("/api/gems", {method: "POST", body: gem}),
        collectGem: (id, lat, lng) =>
            request(`/api/gems/${id}/collect`, {method: "POST", body: {latitude: lat, longitude: lng}}),
        getCollected: () => request("/api/me/collected"),
        resetCollected: () => request("/api/me/collected", {method: "DELETE"})
    };
})();