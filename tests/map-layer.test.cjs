const test = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const source = fs.readFileSync(path.join(__dirname, "../src/main/resources/static/map-layer.js"), "utf8");

function setup(config, failure) {
    const status = { hidden: true, textContent: "" };
    const layers = [];
    const timers = new Set();
    const bounds = {};
    const map = { active: new Set(), removeLayer(layer) { this.active.delete(layer); } };
    const context = {
        window: {}, AbortController,
        document: { getElementById: () => status },
        setTimeout(fn) { timers.add(fn); if (failure === "timeout") fn(); return fn; },
        clearTimeout(fn) { timers.delete(fn); },
        fetch: async (url, options) => {
            assert.equal(url, "/api/config/map");
            assert.equal(options.cache, "no-store");
            if (failure === "network" || options.signal.aborted) throw Error("Unavailable");
            return { ok: failure !== "http", json: async () => config };
        },
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
    return { status, layers, timers, bounds, map, run: () => context.window.HiddenGemMap.addBasemap(map, bounds) };
}

test("MapTiler uses encoded keys, correct tile geometry and attribution", async () => {
    const h = setup({ apiKey: " a&b ", style: "streets-v4" });
    const layer = await h.run();
    assert.equal(layer.url, "https://api.maptiler.com/maps/streets-v4/256/{z}/{x}/{y}.png?key=a%26b");
    assert.equal(layer.options.bounds, h.bounds);
    assert.equal(layer.options.tileSize, 256);
    assert.equal(layer.options.noWrap, true);
    assert.equal(layer.options.crossOrigin, true);
    assert.match(layer.options.attribution, /MapTiler/);
    assert.match(layer.options.attribution, /OpenStreetMap/);
    assert.equal(h.status.hidden, true);
    assert.equal(h.timers.size, 0);
});

test("custom map IDs work and invalid IDs fall back to Streets", async () => {
    for (const [style, expected] of [["custom-map_1", "custom-map_1"], ["https://invalid/style", "streets-v4"]]) {
        const h = setup({ apiKey: "test", style });
        assert.ok((await h.run()).url.includes("/maps/" + expected + "/256/"));
    }
});

test("missing keys, HTTP errors, network errors and timeout use standard tiles", async () => {
    for (const [config, failure] of [[{}, null], [null, null], [{apiKey: " "}, null],
        [{apiKey: "test"}, "http"], [{apiKey: "test"}, "network"], [{apiKey: "test"}, "timeout"]]) {
        const h = setup(config, failure);
        assert.match((await h.run()).url, /tile.openstreetmap.org/);
        assert.equal(h.map.active.size, 1);
        assert.equal(h.status.hidden, false);
        assert.equal(h.timers.size, 0);
    }
});

test("tile errors replace MapTiler only once and report fallback errors", async () => {
    const h = setup({ apiKey: "test" });
    const layer = await h.run();
    layer.events.tileerror();
    layer.events.tileerror();
    assert.equal(h.map.active.has(layer), false);
    assert.equal(h.map.active.size, 1);
    assert.equal(h.layers.length, 2);
    assert.equal(h.layers[1].options.bounds, h.bounds);
    assert.match(h.status.textContent, /standard map/);
    h.layers[1].events.tileerror();
    assert.match(h.status.textContent, /Check your connection/);
});
