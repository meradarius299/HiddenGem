"use strict";

const UNLOCK_RADIUS_M = 75;
const STORAGE_KEY = "hiddenGems.unlocked";
const $ = (id) => document.getElementById(id);

function el(tag, text = "", cls = "") {
    const e = document.createElement(tag);
    if (text) e.textContent = text;
    if (cls) e.className = cls;
    return e;
}

// ---------- progress (browser storage for now; swap for the backend later) ----------
const unlocked = (() => {
    try { return new Set(JSON.parse(localStorage.getItem(STORAGE_KEY)) || []); }
    catch { return new Set(); }
})();

function saveProgress() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify([...unlocked])); }
    catch { /* storage blocked: progress lasts until the page closes */ }
}

// ---------- distance (Haversine formula, result in meters) ----------
function distanceM(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const rad = (d) => (d * Math.PI) / 180;
    const dLat = rad(lat2 - lat1);
    const dLon = rad(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(a));
}

let player = null; // { lat, lon } once we know where the player is
const distTo = (g) => (player ? distanceM(player.lat, player.lon, g.lat, g.lon) : null);
const fmt = (m) => (m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(1)} km`);

// ---------- map ----------
const map = L.map("map");
L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
}).addTo(map);

if (GEMS.length) map.fitBounds(GEMS.map((g) => [g.lat, g.lon]), { padding: [50, 50], maxZoom: 16 });
else map.setView([45, 25], 6);

const playerMarker = L.circleMarker([0, 0], {
    radius: 8, color: "#fff", weight: 3, fillColor: "#1e5fa8", fillOpacity: 1
});

const pinIcon = (done) => L.divIcon({
    className: "",
    html: `<div class="pin ${done ? "done" : ""}">${done ? "✦" : "?"}</div>`,
    iconSize: [34, 34],
    iconAnchor: [17, 17]
});

const markers = new Map();
GEMS.forEach((g) => {
    const m = L.marker([g.lat, g.lon], { icon: pinIcon(false) }).addTo(map);
    m.on("click", () => onGemClick(g));
    markers.set(g.id, m);
});

function onGemClick(g) {
    if (unlocked.has(g.id)) return openStory(g);
    const d = distTo(g);
    const box = el("div");
    box.append(el("strong", g.hint));
    box.append(el("br"));
    box.append(el("span", d === null ? "Waiting for your location..." : `${fmt(d)} away`));
    L.popup().setLatLng([g.lat, g.lon]).setContent(box).openOn(map);
}

// ---------- UI refresh ----------
function updateUI() {
    markers.forEach((m, id) => m.setIcon(pinIcon(unlocked.has(id))));
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
        btn.addEventListener("click", () => map.flyTo([g.lat, g.lon], 17));
        li.append(btn);
        ul.append(li);
    });
}

// ---------- "you're near a gem" popup ----------
const dismissed = new Set(); // gems whose prompt the player closed

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

// ---------- story + quiz dialog ----------
const dlg = $("story");

function closeBtn() {
    const b = el("button", "Close", "plain");
    b.type = "button";
    b.addEventListener("click", () => dlg.close());
    return b;
}

function openStory(g) {
    if (dlg.open) dlg.close();
    dlg.replaceChildren(el("h2", g.name));
    g.story.split("\n\n").forEach((p) => dlg.append(el("p", p)));
    if (unlocked.has(g.id)) dlg.append(closeBtn());
    else renderQuiz(g);
    dlg.showModal();
}

function renderQuiz(g) {
    const wrap = el("div", "", "quiz");
    const feedback = el("p", "", "feedback");
    wrap.append(el("h3", g.quiz.question));
    g.quiz.options.forEach((opt, i) => {
        const b = el("button", opt, "option");
        b.type = "button";
        b.addEventListener("click", () => {
            if (i === g.quiz.answer) collectKey(g);
            else feedback.textContent = "Not quite. Read the story again and try another answer.";
        });
        wrap.append(b);
    });
    wrap.append(feedback);
    dlg.append(wrap, closeBtn());
}

function collectKey(g) {
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

// ---------- player location ----------
function setStatus(msg) { $("status").textContent = msg; }

function setPlayer(lat, lon) {
    player = { lat, lon };
    playerMarker.setLatLng([lat, lon]).addTo(map);
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

// ---------- dev tools ----------
$("fake").addEventListener("change", () =>
    setStatus($("fake").checked ? "Fake location on: click the map to move there." : "Using your real location."));

map.on("click", (e) => {
    if ($("fake").checked) setPlayer(e.latlng.lat, e.latlng.lng);
});

$("reset").addEventListener("click", () => {
    unlocked.clear();
    dismissed.clear();
    saveProgress();
    updateUI();
});

updateUI();