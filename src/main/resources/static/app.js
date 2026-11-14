"use strict";

const GEMS = window.GEMS_DATA;
const UNLOCK_RADIUS_M = 75;
const STORAGE_KEY = "hiddenGems.unlocked." + (window.CURRENT_USER || "guest");
const $ = (id) => document.getElementById(id);

const gemListToggle = $("toggle-gem-list");
gemListToggle.addEventListener("click", () => {
    const open = gemListToggle.getAttribute("aria-expanded") !== "true";
    gemListToggle.setAttribute("aria-expanded", String(open));
    gemListToggle.textContent = open ? "Hide gems" : "Browse gems";
    gemListToggle.closest("aside").classList.toggle("list-open", open);
    $("list").hidden = !open;
    document.querySelector(".dev").hidden = !open;
});

function el(tag, text = "", cls = "") {
    const e = document.createElement(tag);
    if (text) e.textContent = text;
    if (cls) e.className = cls;
    return e;
}

const unlocked = (() => {
    // The server is the source of truth. If we couldn't reach it, use the copy saved in this browser.
    if (Array.isArray(window.COLLECTED_IDS)) return new Set(window.COLLECTED_IDS);
    try { return new Set(JSON.parse(localStorage.getItem(STORAGE_KEY)) || []); }
    catch { return new Set(); }
})();

function saveProgress() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify([...unlocked])); }
    catch {}
}

function distanceM(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const rad = (d) => (d * Math.PI) / 180;
    const dLat = rad(lat2 - lat1);
    const dLon = rad(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
}

let player = null;
const distTo = (g) => (player ? distanceM(player.lat, player.lon, g.lat, g.lon) : null);
const fmt = (m) => (m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(1)} km`);
let routeRequest = 0;

function currentLocation() {
    if (player) return Promise.resolve(player);
    if ($("fake").checked) return Promise.reject(new Error("Click the map to set your starting point."));
    if (!navigator.geolocation) return Promise.reject(new Error("This browser does not provide location access."));
    return new Promise((resolve, reject) => navigator.geolocation.getCurrentPosition(
        pos => { setPlayer(pos.coords.latitude, pos.coords.longitude); resolve(player); },
        () => reject(new Error("Location access is unavailable. Allow location access in your browser and try again.")),
        {enableHighAccuracy: true, timeout: 15000, maximumAge: 0}
    ));
}

function closeGemList() {
    const toggle = $("toggle-gem-list");
    toggle.closest("aside").classList.remove("list-open");
    toggle.setAttribute("aria-expanded", "false");
    toggle.textContent = "Browse gems";
    if (window.matchMedia("(max-width: 800px)").matches) {
        $("list").hidden = true;
        document.querySelector(".dev").hidden = true;
    }
}

async function showRoute(gem) {
    closeGemList();
    const requestId = ++routeRequest;
    map.dismissPopup();
    map.clearRoute();
    map.flyTo([gem.lat, gem.lon], 17);
    try {
        const origin = await currentLocation();
        const url = `https://routing.openstreetmap.de/routed-foot/route/v1/driving/${origin.lon},${origin.lat};${gem.lon},${gem.lat}?overview=full&geometries=geojson&alternatives=3`;
        const response = await fetch(url);
        if (!response.ok) throw new Error("The routing service is unavailable right now.");
        const result = await response.json();
        const route = result.routes?.reduce((shortest, candidate) =>
            !shortest || candidate.distance < shortest.distance ? candidate : shortest, null);
        if (result.code !== "Ok" || !route?.geometry?.coordinates?.length) {
            throw new Error("No walking route was found to this gem.");
        }
        if (requestId !== routeRequest) return;
        map.setRoute(route.geometry.coordinates);
    } catch (error) {
        if (requestId === routeRequest) {
            const status = $("map-status");
            status.textContent = error.message || "Could not calculate the route.";
            status.hidden = false;
        }
    }
}

const map = window.GEM_MAP;
const markers = new Map();
const ratingCache = new Map();
GEMS.forEach((g) => markers.set(g.id, map.addGem(g, () => onGemClick(g))));
const friendsFor = (id) => (window.FRIEND_DISCOVERIES || {})[id] || [];

function loadRatingSummary(gem) {
    if (!ratingCache.has(gem.id)) {
        ratingCache.set(gem.id, API.request(`/api/gems/${gem.id}/reviews`).catch(error => {
            ratingCache.delete(gem.id);
            throw error;
        }));
    }
    return ratingCache.get(gem.id);
}

function onGemClick(g) {
    if (unlocked.has(g.id)) return openStory(g);
    const d = distTo(g);
    const box = el("div");
    box.append(el("strong", g.hint));
    box.append(el("br"));
    box.append(el("span", `Added by ${g.owner || "a town explorer"}`));
    const discoverers = friendsFor(g.id);
    if (discoverers.length) {
        box.append(el("br"));
        box.append(el("span", `Discovered by your friend${discoverers.length > 1 ? "s" : ""}: ${discoverers.join(", ")}`));
    }
    box.append(el("br"));
    box.append(el("span", d === null ? "Waiting for your location..." : `${fmt(d)} away`));
    box.append(el("br"));
    const ratingLine = el("span", window.IS_DEMO ? "Ratings available for saved gems." : "Loading rating…");
    box.append(ratingLine, el("br"));
    if (!window.IS_DEMO) {
        loadRatingSummary(g).then(rating => {
            ratingLine.textContent = rating.reviewCount
                ? `${rating.averageRating.toFixed(1)} / 5 · ${rating.ratingPercentage}% (${rating.reviewCount} reviews)`
                : "No ratings yet";
        }).catch(() => { ratingLine.textContent = "Rating unavailable"; });
    }
    const routeButton = el("button", "Show walking route");
    routeButton.type = "button";
    routeButton.addEventListener("click", () => showRoute(g));
    box.append(routeButton, document.createElement("br"), reviewGemButton(g));
    if (window.IS_ADMIN && !window.IS_DEMO) box.append(document.createElement("br"), adminGemControls(g));
    map.showPopup(g, box);
}

function adminGemControls(g) {
    const controls = el("span", "", "admin-controls");
    const visibility = el("button", g.visible ? "Hide from map" : "Restore to map");
    visibility.type = "button";
    visibility.addEventListener("click", async () => {
        visibility.disabled = true;
        try { await API.setGemVisibility(g.id, !g.visible); location.reload(); }
        catch { visibility.disabled = false; setStatus("Could not update gem visibility."); }
    });
    const remove = el("button", "Delete gem", "plain");
    remove.type = "button";
    remove.addEventListener("click", async () => {
        if (!window.confirm(`Delete “${g.name}” from the map?`)) return;
        remove.disabled = true;
        try { await API.deleteGem(g.id); location.reload(); }
        catch { remove.disabled = false; setStatus("Could not delete this gem."); }
    });
    controls.append(visibility, remove);
    return controls;
}

function updateUI() {
    markers.forEach((m, id) => m.setCollected(unlocked.has(id), friendsFor(id).length > 0));
    $("keys").textContent = `${unlocked.size} / ${GEMS.length} keys`;
    renderList();
    checkNearby();
}

function renderList() {
    const ul = $("list");
    ul.replaceChildren();
    GEMS.forEach((g) => {
        const done = unlocked.has(g.id);
        const d = distTo(g);
        const li = el("li", "", done ? "done" : "");
        const btn = el("button");
        btn.type = "button";
        btn.append(
            el("span", done ? g.name : g.hint, "t"),
            el("span", done ? "Key collected" : d === null ? "Distance unknown" : `${fmt(d)} away`, "s")
        );
        btn.addEventListener("click", () => {
            showRoute(g);
        });
        li.append(btn);
        ul.append(li);
    });
}

const dismissed = new Set();

function checkNearby() {
    const box = $("nearby");
    const near = player
        ? GEMS.filter((g) => !unlocked.has(g.id) && distTo(g) <= UNLOCK_RADIUS_M)
            .sort((a, b) => distTo(a) - distTo(b))[0]
        : null;

    if (!near) { dismissed.clear(); box.hidden = true; return; }
    if (dismissed.has(near.id)) { box.hidden = true; return; }

    $("nearby-text").textContent = `You're close to a hidden gem: ${near.hint}`;
    $("nearby-open").onclick = () => { box.hidden = true; openStory(near); };
    $("nearby-close").onclick = () => { dismissed.add(near.id); box.hidden = true; };
    box.hidden = false;
}

const dlg = $("story");
const reviewsDialog = $("reviews");
let reviewsRequestId = 0;

function closeReviews() {
    if (reviewsDialog.open) reviewsDialog.close();
}

function renderReviews(gem, data) {
    const selected = data.myReview?.rating || 0;
    const reviews = data.reviews || [];
    const close = el("button", "Close", "plain");
    close.type = "button";
    close.addEventListener("click", closeReviews);

    const heading = el("div", "", "review-heading");
    heading.append(el("h2", `Reviews · ${gem.name}`), close);
    const summary = el("p", "", "review-summary");
    summary.textContent = data.reviewCount
        ? `${data.averageRating.toFixed(1)} / 5 · ${data.ratingPercentage}% · ${data.reviewCount} ${data.reviewCount === 1 ? "review" : "reviews"}`
        : "No reviews yet. Be the first to rate this gem.";
    const meter = document.createElement("div");
    meter.className = "review-rating-meter";
    meter.setAttribute("role", "progressbar");
    meter.setAttribute("aria-label", "Average rating percentage");
    meter.setAttribute("aria-valuenow", String(data.ratingPercentage));
    meter.setAttribute("aria-valuemin", "0");
    meter.setAttribute("aria-valuemax", "100");
    const meterFill = document.createElement("span");
    meterFill.style.width = `${data.ratingPercentage}%`;
    meter.append(meterFill);

    const rating = {value: selected};
    const starControls = el("div", "", "review-star-picker");
    starControls.setAttribute("role", "group");
    starControls.setAttribute("aria-label", "Your rating");
    const starButtons = [];
    const updateStars = () => starButtons.forEach((button, index) => {
        button.textContent = index < rating.value ? "★" : "☆";
        button.setAttribute("aria-pressed", String(index + 1 === rating.value));
    });
    for (let i = 1; i <= 5; i++) {
        const star = el("button", "☆", "review-star");
        star.type = "button";
        star.setAttribute("aria-label", `${i} star${i === 1 ? "" : "s"}`);
        star.addEventListener("click", () => { rating.value = i; updateStars(); });
        starButtons.push(star);
        starControls.append(star);
    }
    updateStars();

    const comment = document.createElement("textarea");
    comment.maxLength = 2000;
    comment.rows = 3;
    comment.placeholder = "Share what you thought about this place (optional)";
    comment.setAttribute("aria-label", "Your review");
    comment.value = data.myReview?.comment || "";
    const message = el("p", "", "review-message");
    message.setAttribute("role", "status");
    const submit = el("button", data.myReview ? "Update review" : "Post review");
    submit.type = "submit";
    const form = document.createElement("form");
    form.className = "review-form";
    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (!rating.value) { message.textContent = "Choose a star rating first."; return; }
        submit.disabled = true;
        message.textContent = "Saving your review…";
        try {
            await API.request(`/api/gems/${gem.id}/reviews`, {
                method: "POST", body: {rating: rating.value, comment: comment.value}
            });
            ratingCache.delete(gem.id);
            await openReviews(gem);
        } catch (error) {
            message.textContent = error.status === 401 || error.status === 403
                ? "Your session expired. Log out and log in again."
                : error.status === 400 ? "Use a rating from 1 to 5 stars and a review under 2,000 characters."
                    : "Could not save your review. Please try again.";
            submit.disabled = false;
        }
    });
    form.append(starControls, comment, message, submit);

    const reviewList = el("div", "", "review-list");
    if (!reviews.length) reviewList.append(el("p", "No written reviews yet.", "soc-muted"));
    reviews.forEach(review => {
        const card = el("article", "", "review-card");
        const meta = el("div", "", "review-meta");
        meta.append(el("strong", review.username + (review.userId === data.myReview?.userId ? " · You" : "")),
            el("span", "★".repeat(review.rating) + "☆".repeat(5 - review.rating), "review-display-stars"),
            el("time", new Date(review.updatedAt).toLocaleDateString()));
        card.append(meta);
        if (review.comment) card.append(el("p", review.comment));
        reviewList.append(card);
    });
    return [heading, summary, ...(data.reviewCount ? [meter] : []), form,
        el("h3", `Community reviews (${data.reviewCount})`), reviewList];
}

async function openReviews(gem) {
    const requestId = ++reviewsRequestId;
    if (reviewsDialog.open) reviewsDialog.close();
    reviewsDialog.replaceChildren(el("p", "Loading reviews…", "soc-muted"));
    reviewsDialog.showModal();
    try {
        const data = await API.request(`/api/gems/${gem.id}/reviews`);
        if (requestId === reviewsRequestId && reviewsDialog.open) {
            ratingCache.set(gem.id, Promise.resolve(data));
            reviewsDialog.replaceChildren(...renderReviews(gem, data));
        }
    } catch (error) {
        if (requestId !== reviewsRequestId || !reviewsDialog.open) return;
        const close = el("button", "Close", "plain");
        close.type = "button";
        close.addEventListener("click", closeReviews);
        const heading = el("div", "", "review-heading");
        heading.append(el("h2", `Reviews · ${gem.name}`), close);
        reviewsDialog.replaceChildren(heading,
            el("p", error.status === 404 ? "Reviews are unavailable for this demo gem."
                : "Could not load reviews. Please try again.", "soc-error"));
    }
}

function closeBtn() {
    const b = el("button", "Close", "plain");
    b.type = "button";
    b.addEventListener("click", () => dlg.close());
    return b;
}

function openStory(g) {
    if (dlg.open) dlg.close();
    dlg.replaceChildren(el("h2", g.name));
    dlg.append(el("p", `Added by ${g.owner || "a town explorer"}`, "gem-owner"));
    g.story.split("\n\n").forEach((p) => dlg.append(el("p", p)));
    if (window.IS_ADMIN && !window.IS_DEMO) dlg.append(adminGemControls(g));
    if (unlocked.has(g.id)) {
        dlg.append(reviewGemButton(g), closeBtn());
    }
    else renderQuiz(g);
    dlg.showModal();
}

function reviewGemButton(g) {
    const button = el("button", "Review this gem", "option");
    button.type = "button";
    button.disabled = window.IS_DEMO;
    if (window.IS_DEMO) button.title = "Reviews are available for saved gems.";
    button.addEventListener("click", () => openReviews(g));
    return button;
}

function renderQuiz(g) {
    if (!g.quiz) {
        const collect = el("button", "Collect this key", "option");
        const note = el("p", "", "feedback");
        collect.type = "button";
        collect.addEventListener("click", () => collectKey(g, note));
        dlg.append(collect, note, closeBtn());
        return;
    }
    const wrap = el("div", "", "quiz");
    const feedback = el("p", "", "feedback");
    wrap.append(el("h3", g.quiz.question));
    g.quiz.options.forEach((opt, i) => {
        const b = el("button", opt, "option");
        b.type = "button";
        b.addEventListener("click", () => {
            if (i === g.quiz.answer) collectKey(g, feedback);
            else feedback.textContent = "Not quite. Read the story again and try another answer.";
        });
        wrap.append(b);
    });
    wrap.append(feedback);
    dlg.append(wrap, closeBtn());
}

async function collectKey(g, feedback) {
    if (!window.IS_DEMO) {
        if (!player) { feedback.textContent = "Waiting for your location..."; return; }
        try {
            // the server checks that you are really near the gem before it saves the key
            const result = await API.collectGem(g.id, player.lat, player.lon);
            window.dispatchEvent(new CustomEvent("key-collected", { detail: result || {} })); // rewards.js listens
        } catch (err) {
            feedback.textContent =
                err.status === 400 ? "The server says you are too far from this gem. Get closer and try again."
                    : err.status === 401 || err.status === 403 ? "Your session expired. Log out and log in again."
                        : "Could not save your key. Is the backend running?";
            return;
        }
    }
    unlocked.add(g.id);
    saveProgress();
    dlg.replaceChildren(
        el("h2", "Key collected"),
        el("p", `You earned the key for ${g.name}. ${unlocked.size} of ${GEMS.length} keys found.`)
    );
    if (unlocked.size === GEMS.length) {
        dlg.append(el("p", "You found every hidden gem. The town has no secrets left from you.", "win"));
    }
    dlg.append(closeBtn());
    updateUI();
}

function setStatus(msg) { $("status").textContent = msg; }

function setPlayer(lat, lon) {
    player = { lat, lon };
    map.setPlayer(lat, lon);
    updateUI();
}

if ("geolocation" in navigator) {
    navigator.geolocation.watchPosition(
        (pos) => { if (!$("fake").checked) setPlayer(pos.coords.latitude, pos.coords.longitude); },
        (err) => setStatus(`Location unavailable (${err.message}). Use the fake location option to test.`),
        { enableHighAccuracy: true, maximumAge: 5000 }
    );
} else {
    setStatus("This browser has no geolocation. Use the fake location option to test.");
}

$("fake").addEventListener("change", () =>
    setStatus($("fake").checked ? "Fake location on: click the map to move there." : "Using your real location."));

const CATEGORIES = ["Museum", "Park", "Street art", "Viewpoint", "Cafe", "Other"];
const addBtn = $("add-gem");
let picking = false;

function setPicking(on) {
    picking = on;
    map.getContainer().style.cursor = on ? "crosshair" : "";
    addBtn.textContent = on ? "Click the map to place it (cancel)" : "+ Add a gem";
}

addBtn.addEventListener("click", () => setPicking(!picking));

function openGemForm(lat, lon) {
    if (dlg.open) dlg.close();
    dlg.replaceChildren(el("h2", "Add a hidden gem"));

    const field = (label, control) => {
        const l = el("label");
        l.append(el("span", label), control);
        return l;
    };
    const name = el("input");
    name.maxLength = 120;
    const category = el("select");
    CATEGORIES.forEach((c) => category.append(new Option(c, c)));
    const story = el("textarea");
    story.rows = 6;
    story.maxLength = 5000;
    const msg = el("p", "", "feedback");
    const save = el("button", "Save gem");
    save.type = "submit";
    const cancel = el("button", "Cancel", "plain");
    cancel.type = "button";
    cancel.addEventListener("click", () => dlg.close());
    const actions = el("div", "", "actions");
    actions.append(save, cancel);

    const form = el("form", "", "gem-form");
    form.noValidate = true;
    form.append(
        field("Name", name),
        field("Category", category),
        field("Story (what makes this place special?)", story),
        el("p", `Location: ${lat.toFixed(5)}, ${lon.toFixed(5)}`),
        msg,
        actions
    );

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        if (!name.value.trim()) { msg.textContent = "Give the gem a name."; return; }
        save.disabled = true;
        msg.textContent = "";
        try {
            await API.createGem({
                name: name.value.trim(),
                description: story.value.trim(),
                latitude: lat,
                longitude: lon,
                category: category.value
            });
            location.reload();
        } catch (err) {
            msg.textContent =
                err.status === 401 || err.status === 403 ? "Your session expired. Log out and log in again."
                    : err.status === 400 ? "Check the fields: the name is required."
                        : "Could not save. Is the backend running?";
            save.disabled = false;
        }
    });

    dlg.append(form);
    dlg.showModal();
}

map.onClick((e) => {
    if (picking) {
        setPicking(false);
        openGemForm(e.latlng.lat, e.latlng.lng);
    } else if ($("fake").checked) {
        setPlayer(e.latlng.lat, e.latlng.lng);
    }
});

$("reset").addEventListener("click", async () => {
    if (!window.IS_DEMO) {
        try { await API.resetCollected(); }
        catch { setStatus("Could not reset your keys on the server."); return; }
        window.dispatchEvent(new Event("progress-reset"));
    }
    unlocked.clear();
    dismissed.clear();
    saveProgress();
    updateUI();
});

updateUI();
