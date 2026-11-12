# Hidden Gems

A Spring Boot application for discovering nearby places and collecting their stories.

## MapTiler setup

The map uses Leaflet with MapTiler Streets raster tiles. Existing gem markers,
location tracking, adding gems and collection remain in Leaflet. OpenStreetMap
is used if the key is absent, configuration fails, or MapTiler tiles fail.

1. Create a browser map key at https://cloud.maptiler.com/account/keys/.
2. Set `MAPTILER_API_KEY` in the environment used to start the backend.
3. Restart the backend and reload the app.

PowerShell, from the repository directory, with the existing database/auth settings configured:

```powershell
$env:MAPTILER_API_KEY = "your-maptiler-key"
.\mvnw.cmd spring-boot:run
```

For an IDE run configuration, add `MAPTILER_API_KEY=your-maptiler-key` to its environment variables.
Optionally set `MAPTILER_MAP_ID` to a MapTiler map ID supporting raster tiles (default: `streets-v4`).
Use an ID, not a style URL.

The public `GET /api/config/map` endpoint returns only the map key and style.
The key is visible in browser network requests by design. Use a dedicated map key
and restrict its **Allowed HTTP origins** in MapTiler to your deployed domain.
Use a separate development key allowing `localhost` or `127.0.0.1` as appropriate.
Do not commit actual keys.

- [MapTiler Leaflet raster integration](https://docs.maptiler.com/leaflet/examples/raster-tiles-in-leaflet-js/)
- [Key and origin settings](https://docs.maptiler.com/guides/developer/credentials/api-key/)

## Verification

Run frontend tests with `node --test tests/map-layer.test.cjs` (Node 18+).
Run Java tests with `.\mvnw.cmd test` (JDK 26, as configured in pom.xml).

Log in and check desktop and mobile layouts:
- A valid key displays Streets tiles and MapTiler/OpenStreetMap attribution.
- A missing or rejected key displays the standard map and a status message.
- Gem clicks, list navigation, fake location, adding gems and collection still work.
- Zoom controls, the legend and attribution remain readable.
