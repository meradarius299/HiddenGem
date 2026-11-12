"use strict";

window.HiddenGemMap = {
    async addBasemap(map, bounds) {
        const status = document.getElementById("map-status");
        const notify = (message) => {
            status.textContent = message;
            status.hidden = !message;
        };
        const osmAttribution = '<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">&copy; OpenStreetMap contributors</a>';
        const common = { maxZoom: 19, noWrap: true, bounds };
        let fallbackLayer;
        const useFallback = (message) => {
            if (fallbackLayer) return fallbackLayer;
            notify(message);
            fallbackLayer = L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
                ...common, attribution: osmAttribution
            });
            fallbackLayer.on("tileerror", () => notify("Map tiles are unavailable. Check your connection and reload."));
            fallbackLayer.addTo(map);
            return fallbackLayer;
        };

        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);
        let config;
        try {
            const response = await fetch("/api/config/map", {
                signal: controller.signal, cache: "no-store"
            });
            if (!response.ok) throw new Error("Map configuration unavailable");
            config = await response.json();
        } catch {
            return useFallback("Using the standard map. Refresh to try the enhanced map again.");
        } finally {
            clearTimeout(timeout);
        }

        const key = typeof config?.apiKey === "string" ? config.apiKey.trim() : "";
        if (!key) return useFallback("Using the standard map.");
        const style = typeof config.style === "string" && /^[a-zA-Z0-9_-]+$/.test(config.style)
            ? config.style : "streets-v4";
        const layer = L.tileLayer(
            `https://api.maptiler.com/maps/${style}/256/{z}/{x}/{y}.png?key=${encodeURIComponent(key)}`,
            {
                ...common, tileSize: 256, crossOrigin: true,
                attribution: '<a href="https://www.maptiler.com/copyright/" target="_blank" rel="noopener">&copy; MapTiler</a> ' + osmAttribution
            }
        );
        layer.once("tileerror", () => {
            map.removeLayer(layer);
            useFallback("The enhanced map is unavailable. Showing the standard map.");
        });
        notify("");
        layer.addTo(map);
        return layer;
    }
};
