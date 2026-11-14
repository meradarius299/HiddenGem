const test = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const source = fs.readFileSync(path.join(__dirname, "../src/main/resources/static/map-layer.js"), "utf8");

function setup() {
    const status = { hidden: true, textContent: "" };
    const layers = [];
    const bounds = {};
    const map = { active: new Set(), removeLayer(layer) { this.active.delete(layer); } };
    const context = {
        window: {},
        document: { getElementById: () => status },
        L: { tileLayer(url, options) {
            const layer = {
                url, options, events: {},
                on(event, fn) { this.events[event] = fn; return this; },
                once(event, fn) { this.events[event] = fn; return this; },
                addTo(target) { target.active.add(this); return this; }
            };
            layers.push(layer);
            return layer;
        }}
    };
    vm.runInNewContext(source, context);
    return { status, layers, bounds, map, run: () => context.window.HiddenGemMap.addBasemap(map, bounds) };
}

test("OpenStreetMap provides the Leaflet fallback without an API key", () => {
    const h = setup();
    const layer = h.run();
    assert.equal(layer.url, "https://tile.openstreetmap.org/{z}/{x}/{y}.png");
    assert.equal(layer.options.bounds, h.bounds);
    assert.equal(layer.options.noWrap, true);
    assert.match(layer.options.attribution, /OpenStreetMap/);
    assert.equal(h.status.hidden, true);
    assert.equal(h.map.active.size, 1);
});

test("OpenStreetMap tile errors are reported", () => {
    const h = setup();
    const layer = h.run();
    layer.events.tileerror();
    assert.equal(h.map.active.has(layer), true);
    assert.equal(h.map.active.size, 1);
    assert.match(h.status.textContent, /Check your connection/);
});
