"use strict";

window.HiddenGemMap = {
    addBasemap(map, bounds) {
        const status = document.getElementById("map-status");
        const notify = (message) => {
            status.textContent = message;
            status.hidden = !message;
        };
        const osmAttribution = '<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">&copy; OpenStreetMap contributors</a>';
        const layer = L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19, noWrap: true, bounds, attribution: osmAttribution
        });
        layer.on("tileerror", () => notify("Map tiles are unavailable. Check your connection and reload."));
        notify("");
        layer.addTo(map);
        return layer;
    }
};
