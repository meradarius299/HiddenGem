# Hidden Gems

Discover nearby places and collect their stories.

## Google 3D globe

With `GOOGLE_MAPS_API_KEY` set, the app opens a Google Maps 3D globe.
Use **Explore gems** to fly to the first gem with an angled camera, click gems in
the list to visit them, and **Globe** to return to the planet view. The map supports
rotation, tilt, zoom, interactive gem markers and your current position.
Google's built-in controls and attribution remain visible.

Google renders photorealistic buildings only where it has 3D coverage. Elsewhere
you see satellite imagery and terrain. This implementation uses the Maps JavaScript
API beta channel with the `maps3d` and `marker` libraries.

### Setup

1. Create/select a Google Cloud project, enable billing and enable **Maps JavaScript API**.
2. Create a browser API key. Restrict it to Maps JavaScript API and your website's
   HTTP referrers (including your local development URL when needed).
3. Set the key in the backend environment and restart.

PowerShell from the repository directory, with the existing database/auth settings configured:

```powershell
$env:GOOGLE_MAPS_API_KEY = "your-google-maps-key"
.\mvnw.cmd spring-boot:run
```

For an IDE, add `GOOGLE_MAPS_API_KEY=your-google-maps-key` to its run configuration.
Map loads can incur Google Maps Platform charges. Review pricing and quotas in your
Google Cloud project. No key or billing configuration is created by this code.

The public `GET /api/config/map` endpoint exposes only browser map settings.
Browser API keys are visible in network requests by design; restrict them at the
provider and do not commit actual keys.

- [Google 3D maps and coverage](https://developers.google.com/maps/documentation/javascript/3d/overview)
- [Google API key setup](https://developers.google.com/maps/documentation/javascript/get-api-key)
- [Google usage and billing](https://developers.google.com/maps/documentation/javascript/usage-and-billing)

## 2D fallback

If no Google key is configured, initialization fails, or the browser cannot render
3D, the app falls back to its Leaflet map and labels it as 2D.
Optionally set `MAPTILER_API_KEY` for MapTiler Streets fallback tiles and
`MAPTILER_MAP_ID` for a custom raster map ID (default `streets-v4`).
Without a MapTiler key, or if its tiles fail, OpenStreetMap is used.
Restrict the MapTiler browser key to allowed origins in its dashboard.

## Verification

Run `node --test tests/*.test.cjs` with Node 18+ and `.\mvnw.cmd test` with JDK 26.

Before merging, test with a valid Google key in a browser at desktop and mobile sizes:
- The globe renders; Explore gems and Globe move the camera in both directions.
- Rotate, tilt and zoom near a Google-covered city to check 3D buildings.
- Clicking a gem shows its hint/story; collected markers change color.
- List and social-panel navigation fly to the chosen gem.
- Real/fake location, click-to-add, collecting and resetting keys still work.
- Clicking the sky does not add a gem or change your fake location.
- Missing/rejected keys and blocked Google scripts fall back to 2D.
- Attribution, native controls, legend and gem details remain readable.
- Reduced-motion preference disables flight animation.
