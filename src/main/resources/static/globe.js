"use strict";

window.HiddenGemGlobe = (() => {
    const status = (message) => {
        const node = document.getElementById("map-status");
        node.textContent = message;
        node.hidden = !message;
    };
    const deadline = (promise, ms) => new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error("Map timed out")), ms);
        promise.then(value => { clearTimeout(timer); resolve(value); },
            error => { clearTimeout(timer); reject(error); });
    });

    async function loadGoogle(key) {
        if (!window.google?.maps?.importLibrary) {
            await new Promise((resolve, reject) => {
                const script = document.createElement("script");
                const previousAuthFailure = window.gm_authFailure;
                const timer = setTimeout(() => finish(new Error("Google Maps timed out")), 15000);
                let finished = false;
                function finish(error) {
                    if (finished) return;
                    finished = true;
                    clearTimeout(timer);
                    window.gm_authFailure = previousAuthFailure;
                    // A late script response may still invoke its callback.
                    window.hiddenGemGoogleReady = () => {};
                    script.onerror = null;
                    if (error) { script.remove(); reject(error); } else resolve();
                }
                window.hiddenGemGoogleReady = () => finish();
                window.gm_authFailure = () => finish(new Error("Google Maps authorization failed"));
                script.async = true;
                script.src = "https://maps.googleapis.com/maps/api/js?key=" + encodeURIComponent(key)
                    + "&v=beta&loading=async&libraries=maps3d,marker&callback=hiddenGemGoogleReady";
                script.onerror = () => finish(new Error("Google Maps could not load"));
                document.head.append(script);
            });
        }
        return deadline(Promise.all([
            window.google.maps.importLibrary("maps3d"),
            window.google.maps.importLibrary("marker")
        ]), 15000);
    }

    function leaflet(gems) {
        const bounds = L.latLngBounds([[-85, -180], [85, 180]]);
        const map = L.map("map", { maxBounds: bounds, maxBoundsViscosity: 1 });
        const fitWorld = () => map.setMinZoom(map.getBoundsZoom(bounds, true));
        fitWorld();
        map.on("resize", fitWorld);
        HiddenGemMap.addBasemap(map, bounds);
        L.control.scale({ imperial: false, position: "bottomleft" }).addTo(map);
        if (gems.length) map.fitBounds(gems.map(g => [g.lat, g.lon]), { padding: [50, 50], maxZoom: 16 });
        else map.setView([45, 25], 6);
        const player = L.circleMarker([0, 0], {
            radius: 8, color: "#fff", weight: 3, fillColor: "#1e5fa8", fillOpacity: 1
        });
        const icon = done => L.divIcon({
            className: "", html: '<div class="pin ' + (done ? "done" : "") + '">' + (done ? "✦" : "?") + '</div>',
            iconSize: [34, 34], iconAnchor: [17, 17]
        });
        return {
            addGem(g, onClick) {
                const marker = L.marker([g.lat, g.lon], { icon: icon(false) }).addTo(map);
                marker.on("click", onClick);
                return { setCollected: done => marker.setIcon(icon(done)) };
            },
            flyTo: (position, zoom) => map.flyTo(position, zoom),
            setPlayer: (lat, lng) => player.setLatLng([lat, lng]).addTo(map),
            showPopup: (g, content) => L.popup().setLatLng([g.lat, g.lon]).setContent(content).openOn(map),
            getContainer: () => map.getContainer(),
            onClick: fn => map.on("click", fn)
        };
    }

    async function googleGlobe(gems, key) {
        const [{ Map3DElement, Marker3DInteractiveElement, Marker3DElement }, { PinElement }] = await loadGoogle(key);
        const host = document.getElementById("map");
        const first = gems[0] || { lat: 45, lon: 25 };
        const center = { lat: first.lat, lng: first.lon };
        const globe = new Map3DElement({
            center, range: 20000000, tilt: 0, heading: 0,
            mode: "HYBRID", gestureHandling: "GREEDY"
        });
        globe.style.width = "100%";
        globe.style.height = "100%";
        // Wait for actual rendering, so invalid keys and unsupported devices can fall back.
        try {
            await new Promise((resolve, reject) => {
                const timer = setTimeout(() => finish(new Error("3D rendering timed out")), 25000);
                function finish(error) {
                    clearTimeout(timer);
                    globe.removeEventListener("gmp-error", failed);
                    globe.removeEventListener("gmp-steadychange", ready);
                    if (error) reject(error); else resolve();
                }
                const failed = () => finish(new Error("3D rendering unavailable"));
                const ready = event => { if (event.isSteady) finish(); };
                globe.addEventListener("gmp-error", failed);
                globe.addEventListener("gmp-steadychange", ready);
                host.replaceChildren(globe);
            });
        } catch (error) {
            globe.remove();
            throw error;
        }
        globe.addEventListener("gmp-error", () => status("The 3D view is unavailable. Reload to try again."));
        const detail = document.getElementById("map-detail");
        const fly = camera => {
            detail.hidden = true;
            globe.stopCameraAnimation();
            globe.flyCameraTo({
                endCamera: camera,
                durationMillis: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : 1800
            });
        };
        const explore = (lat, lng) => fly({ center: { lat, lng, altitude: 0 }, altitudeMode: "RELATIVE_TO_GROUND", range: 4000, tilt: 60, heading: 0 });
        document.getElementById("view-globe").onclick = () =>
            fly({ center: globe.center, range: 20000000, tilt: 0, heading: 0 });
        document.getElementById("view-gems").onclick = () => explore(first.lat, first.lon);
        document.getElementById("map-views").hidden = false;
        let player;
        return {
            addGem(g, onClick) {
                const marker = new Marker3DInteractiveElement({
                    position: { lat: g.lat, lng: g.lon },
                    altitudeMode: "CLAMP_TO_GROUND", drawsWhenOccluded: true, title: g.hint
                });
                const pin = new PinElement({ background: "#a9741a", borderColor: "#fff", glyphColor: "#fff", glyphText: "?", scale: 1.2 });
                marker.append(pin);
                marker.addEventListener("gmp-click", event => { event.stopPropagation(); onClick(); });
                globe.append(marker);
                return { setCollected(done) {
                    pin.background = done ? "#2e6b57" : "#a9741a";
                    pin.glyphText = done ? "✦" : "?";
                    marker.title = done ? g.name : g.hint;
                }};
            },
            flyTo: ([lat, lng]) => explore(lat, lng),
            setPlayer(lat, lng) {
                if (!player) {
                    player = new Marker3DElement({ altitudeMode: "CLAMP_TO_GROUND", drawsWhenOccluded: true, label: "You" });
                    player.append(new PinElement({ background: "#1e5fa8", glyphColor: "#fff", glyphText: "•" }));
                    globe.append(player);
                }
                player.position = { lat, lng };
            },
            showPopup(g, content) {
                const close = document.createElement("button");
                close.type = "button";
                close.textContent = "Close";
                close.onclick = () => { detail.hidden = true; };
                detail.replaceChildren(content, close);
                detail.hidden = false;
            },
            getContainer: () => host,
            onClick(fn) {
                globe.addEventListener("gmp-click", event => {
                    const position = event.position;
                    // Sky clicks have no geographical position.
                    if (position && Number.isFinite(position.lat) && Number.isFinite(position.lng)) {
                        fn({ latlng: { lat: position.lat, lng: position.lng } });
                    }
                });
            }
        };
    }

    async function create(gems) {
        status("Loading globe…");
        let config;
        try {
            const controller = new AbortController();
            const timer = setTimeout(() => controller.abort(), 8000);
            try {
                const response = await fetch("/api/config/map", { signal: controller.signal, cache: "no-store" });
                if (!response.ok) throw new Error("Map settings unavailable");
                config = await response.json();
            } finally { clearTimeout(timer); }
            const key = typeof config?.googleApiKey === "string" ? config.googleApiKey.trim() : "";
            if (key) {
                const map = await googleGlobe(gems, key);
                status("");
                return map;
            }
        } catch {
            // The existing map remains usable when Google cannot initialize.
        }
        document.getElementById("map-views").hidden = true;
        const fallback = leaflet(gems);
        // Keep this separate from the basemap's tile status.
        const note = document.createElement("p");
        note.className = "map-mode-note";
        note.textContent = "2D map · 3D globe unavailable";
        document.querySelector(".map-panel").append(note);
        return fallback;
    }

    return { create };
})();
