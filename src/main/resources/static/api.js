"use strict";

const API = (() => {
    const TOKEN = "hg.token", USER = "hg.user", ROLES = "hg.roles";
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
            let detail = "";
            try {
                const payload = await res.json();
                detail = payload.detail || payload.message || "";
            } catch { /* response may have no JSON body */ }
            const err = new Error(`HTTP ${res.status}`);
            err.status = res.status;
            err.detail = detail;
            throw err;
        }
        return res.status === 204 ? null : res.json();
    }

    const save = (r) => {
        store.set(TOKEN, r.token);
        store.set(USER, r.username);
        store.set(ROLES, JSON.stringify(r.roles || []));
        return r;
    };

    return {
        request,
        isLoggedIn: () => !!store.get(TOKEN),
        username: () => store.get(USER) || "",
        isAdmin: () => { try { return JSON.parse(store.get(ROLES) || "[]").includes("ROLE_ADMIN"); } catch { return false; } },
        logout: () => {
            store.del(TOKEN);
            store.del(USER);
            store.del(ROLES);
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
        deleteGem: (id) => request(`/api/gems/${id}`, {method: "DELETE"}),
        setGemVisibility: (id, visible) => request(`/api/gems/${id}/visibility?visible=${visible}`, {method: "PATCH"}),
        getHiddenGems: () => request("/api/gems/hidden"),
        collectGem: (id, lat, lng) =>
            request(`/api/gems/${id}/collect`, {method: "POST", body: {latitude: lat, longitude: lng}}),
        getCollected: () => request("/api/me/collected"),
        resetCollected: () => request("/api/me/collected", {method: "DELETE"})
    };
})();
