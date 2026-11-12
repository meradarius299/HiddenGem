const test = require("node:test");
const assert = require("node:assert/strict");
const vm = require("node:vm");
const fs = require("node:fs");
const path = require("node:path");
const source = fs.readFileSync(path.join(__dirname, "../src/main/resources/static/globe.js"), "utf8");

function harness({ key = "test", renderError = false, scriptError = false, configError = false, reducedMotion = false } = {}) {
    class Element {
        constructor(options = {}) { Object.assign(this, options); this.style = {}; this.children = []; this.listeners = {}; this.hidden = true; }
        append(...children) { this.children.push(...children); }
        replaceChildren(...children) {
            this.children = children;
            if (children[0] instanceof Map3D) {
                children[0].emit(renderError ? "gmp-error" : "gmp-steadychange", { isSteady: true });
            }
        }
        remove() { this.removed = true; }
        addEventListener(type, fn) { (this.listeners[type] ||= new Set()).add(fn); }
        removeEventListener(type, fn) { this.listeners[type]?.delete(fn); }
        emit(type, event) { for (const fn of this.listeners[type] || []) fn(event); }
    }
    let globe;
    class Map3D extends Element {
        constructor(options) { super(options); globe = this; }
        stopCameraAnimation() {}
        flyCameraTo(options) { this.flight = options; }
    }
    class Marker extends Element {}
    class Pin extends Element {}
    const nodes = {};
    const get = id => nodes[id] ||= new Element();
    const scripts = [];
    const leafletMap = {
        setMinZoom() {}, getBoundsZoom: () => 1, on() {}, fitBounds() {}, setView() {},
        flyTo(position, zoom) { this.flight = { position, zoom }; },
        getContainer: () => get("map")
    };
    const timers = new Set();
    const context = {
        window: {
            matchMedia: () => ({ matches: reducedMotion }),
            google: scriptError ? undefined : { maps: { importLibrary: async name => name === "maps3d"
                ? { Map3DElement: Map3D, Marker3DInteractiveElement: Marker, Marker3DElement: Marker }
                : { PinElement: Pin } }}
        },
        document: {
            getElementById: get, querySelector: () => get("panel"), createElement: () => new Element(),
            head: { append(script) { scripts.push(script); if (scriptError) script.onerror(); } }
        },
        fetch: async () => ({ ok: !configError, json: async () => ({ googleApiKey: key }) }),
        AbortController,
        setTimeout(fn) { timers.add(fn); return fn; },
        clearTimeout(fn) { timers.delete(fn); },
        HiddenGemMap: { addBasemap() {} },
        L: { latLngBounds: () => ({}), map: () => leafletMap, circleMarker: () => ({}), control: { scale: () => ({ addTo() {} }) } }
    };
    vm.runInNewContext(source, context);
    return { get, timers, scripts, leafletMap, get globe() { return globe; }, create: () => context.window.HiddenGemGlobe.create([{lat: 45, lon: 25}]) };
}

test("globe starts at planet scale and navigation supports terrain and reduced motion", async () => {
    const h = harness({ reducedMotion: true });
    const map = await h.create();
    assert.equal(h.globe.range, 20000000);
    assert.equal(h.globe.mode, "HYBRID");
    assert.equal(h.get("map-views").hidden, false);
    map.flyTo([46, 24], 17);
    assert.equal(h.globe.flight.endCamera.center.lat, 46);
    assert.equal(h.globe.flight.endCamera.altitudeMode, "RELATIVE_TO_GROUND");
    assert.equal(h.globe.flight.endCamera.tilt, 60);
    assert.equal(h.globe.flight.durationMillis, 0);
    h.get("view-globe").onclick();
    assert.equal(h.globe.flight.endCamera.range, 20000000);
    h.get("view-gems").onclick();
    assert.equal(h.globe.flight.endCamera.center.lat, 45);
    assert.equal(h.timers.size, 0);
});

test("gem clicks, collection state and player updates preserve map interactions", async () => {
    const h = harness();
    const map = await h.create();
    let clicked = 0, stopped = false;
    const marker = map.addGem({lat: 45, lon: 25, hint: "Hint", name: "Gem"}, () => clicked++);
    const element = h.globe.children[0];
    element.emit("gmp-click", {stopPropagation() { stopped = true; }});
    assert.equal(clicked, 1); assert.equal(stopped, true);
    marker.setCollected(true);
    assert.equal(element.title, "Gem");
    assert.equal(element.children[0].background, "#2e6b57");
    map.setPlayer(46, 24); map.setPlayer(47, 23);
    assert.equal(h.globe.children.length, 2);
    assert.equal(h.globe.children[1].position.lat, 47);
    let count = 0;
    map.onClick(e => { count++; assert.equal(e.latlng.lng, 25); });
    h.globe.emit("gmp-click", {});
    h.globe.emit("gmp-click", {position: {lat: 45, lng: 25}});
    assert.equal(count, 1);
});

test("missing key, configuration error, script failure and renderer failure fall back", async () => {
    for (const options of [{key: ""}, {configError: true}, {scriptError: true}, {renderError: true}]) {
        const h = harness(options);
        const map = await h.create();
        assert.equal(h.get("map-views").hidden, true);
        assert.equal(h.get("panel").children[0].textContent, "2D map · 3D globe unavailable");
        map.flyTo([45, 25], 17);
        assert.equal(h.leafletMap.flight.zoom, 17);
        if (options.renderError) assert.equal(h.globe.removed, true);
        assert.equal(h.timers.size, 0);
    }
});
